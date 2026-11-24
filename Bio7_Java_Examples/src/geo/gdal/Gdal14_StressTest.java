package geo.gdal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import com.eco.bio7.gdal.CoordinateTransform;
import com.eco.bio7.gdal.Feature;
import com.eco.bio7.gdal.FieldType;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataType;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.GdalException;
import com.eco.bio7.gdal.GeometryType;
import com.eco.bio7.gdal.SpatialReference;
import com.eco.bio7.gdal.VectorLayer;

/*
 * A stress test of the GDAL bridge (FFM): many open/close cycles, a large
 * raster written and read, random windows, several threads, repeated warps,
 * many vector features with filters and SQL, a million coordinate
 * transformations, storms of errors (missing and corrupt files, wrong SQL and
 * coordinate systems), the memory over repeated rounds (a leak shows as
 * growing native memory) and the compatibility layer (org.gdal).
 *
 * Every result is checked (values, counts, sums, round trips). The
 * measurements are printed and written as JSON (stresstest_results.json in the
 * folder of the GDAL examples). Run Gdal01_RasterBasics first. It takes about
 * one to three minutes and needs about 300 MB of disk space (deleted at the end).
 */
public class Gdal14_StressTest {

	/** SCALE 1: the default; 0.25: a quick run; 2: twice the work. */
	static double SCALE = 1.0;

	private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
	private static final List<String> FAILURES = new ArrayList<>();
	private static final Map<String, Boolean> CHECKS = new LinkedHashMap<>();

	public static void main(String[] args) throws Exception {
		if (!GdalSetup.load()) {
			return;
		}
		Path dir = GdalSetup.folder();
		Path dem = dir.resolve("dem_utm.tif");
		if (!Files.exists(dem)) {
			System.err.println("Please run Gdal01_RasterBasics first.");
			return;
		}
		Path work = Files.createDirectories(dir.resolve("stresstest"));
		long start = System.nanoTime();

		Map<String, Object> env = new LinkedHashMap<>();
		env.put("gdal", Gdal.version());
		env.put("library", String.valueOf(Gdal.library()));
		env.put("java", System.getProperty("java.version"));
		env.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
		env.put("cpus", Runtime.getRuntime().availableProcessors());
		env.put("maxHeapMB", Runtime.getRuntime().maxMemory() / 1048576);
		env.put("scale", SCALE);
		RESULTS.put("environment", env);
		System.out.println("GDAL stress test: GDAL " + Gdal.version() + ", Java " + env.get("java") + ", " + env.get("cpus") + " CPUs\n");

		openClose(dem);
		Path big = largeRaster(work);
		randomWindows(big);
		threads(big);
		warps(dem);
		vectors(work);
		transformations();
		errors(work, dem);
		memoryRounds(dem);
		compatibility(big);
		terrainTools(dem);

		for (Path p : Files.list(work).toList()) {
			Files.deleteIfExists(p);
		}
		Files.deleteIfExists(work);

		double seconds = (System.nanoTime() - start) / 1e9;
		RESULTS.put("totalSeconds", seconds);
		RESULTS.put("checks", CHECKS);
		RESULTS.put("failures", FAILURES);
		Path json = dir.resolve("stresstest_results.json");
		Files.writeString(json, toJson(RESULTS, ""));
		System.out.printf(Locale.ROOT, "%nTotal: %.1f s, %s%nResults: %s%n", seconds, FAILURES.isEmpty() ? "all checks passed" : FAILURES.size() + " FAILURES: " + FAILURES, json);
	}

	/* ---------------- The tests ---------------- */

	/** Open, query and close the same file many times. */
	static void openClose(Path dem) {
		int n = (int) (3000 * SCALE);
		List<double[]> series = new ArrayList<>();   // iteration, seconds, RSS MB
		long t0 = System.nanoTime();
		long checksum = 0;
		for (int i = 1; i <= n; i++) {
			try (GdalDataset ds = Gdal.open(dem.toString())) {
				checksum += ds.width() + ds.height() + (long) ds.geoTransform()[1] + ds.projection().length() + (ds.band(1).noData() != null ? 1 : 0);
			}
			if (i % Math.max(1, n / 10) == 0) {
				series.add(new double[] { i, (System.nanoTime() - t0) / 1e9, rssMB() });
			}
		}
		double s = (System.nanoTime() - t0) / 1e9;
		check("open/close: the same values each time", checksum == (long) n * (400 + 300 + 10 + projectionLength(dem) + 1));
		Map<String, Object> r = result("Open / close", n, s, "cycles");
		r.put("series", series);
		print(r);
	}

	/** A large raster (GeoTIFF, tiled, compressed) written in blocks and read again. */
	static Path largeRaster(Path work) {
		int size = (int) Math.round(6000 * Math.sqrt(SCALE));
		Path file = work.resolve("large.tif");
		long t0 = System.nanoTime();
		try (GdalDataset ds = Gdal.create(file.toString(), "GTiff", size, size, 1, GdalDataType.FLOAT32, "TILED=YES", "COMPRESS=DEFLATE", "BIGTIFF=IF_SAFER")) {
			ds.setGeoTransform(new double[] { 500000, 1, 0, 5300000, 0, -1 });
			ds.setProjection("EPSG:32632");
			int rows = 512;
			for (int y = 0; y < size; y += rows) {
				int h = Math.min(rows, size - y);
				float[] block = new float[size * h];
				for (int j = 0; j < h; j++) {
					for (int x = 0; x < size; x++) {
						block[j * size + x] = value(x, y + j);
					}
				}
				ds.band(1).write(0, y, size, h, block);
			}
		}
		double write = (System.nanoTime() - t0) / 1e9;
		double mb = (double) size * size * 4 / 1048576;

		t0 = System.nanoTime();
		float[] all;
		try (GdalDataset ds = Gdal.open(file.toString())) {
			all = ds.band(1).readFloats();
		}
		double read = (System.nanoTime() - t0) / 1e9;
		boolean ok = all.length == size * size;
		for (int i = 0; ok && i < all.length; i += 997) {
			ok = all[i] == value(i % size, i / size);
		}
		check("large raster: the values read are the values written", ok);
		long fileBytes = 0;
		try {
			fileBytes = Files.size(file);
		} catch (Exception e) {
			/* size unknown */
		}
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("name", "Large raster");
		r.put("pixels", (long) size * size);
		r.put("size", size + " x " + size);
		r.put("dataMB", mb);
		r.put("fileMB", fileBytes / 1048576.0);
		r.put("writeSeconds", write);
		r.put("readSeconds", read);
		r.put("writeMBps", mb / write);
		r.put("readMBps", mb / read);
		r.put("rssMB", rssMB());
		RESULTS.put("largeRaster", r);
		System.out.printf(Locale.ROOT, "%-24s %s (%.0f MB, file %.0f MB): write %.1f s (%.0f MB/s), read %.1f s (%.0f MB/s)%n", "Large raster", r.get("size"), mb,
				fileBytes / 1048576.0, write, mb / write, read, mb / read);
		return file;
	}

	/** Random windows of 256 x 256 pixels. */
	static void randomWindows(Path big) {
		int n = (int) (4000 * SCALE);
		Random random = new Random(1);
		long t0 = System.nanoTime();
		int bad = 0;
		try (GdalDataset ds = Gdal.open(big.toString())) {
			int size = ds.width();
			for (int i = 0; i < n; i++) {
				int x = random.nextInt(size - 256), y = random.nextInt(size - 256);
				float[] w = ds.band(1).readFloats(x, y, 256, 256);
				if (w[0] != value(x, y) || w[w.length - 1] != value(x + 255, y + 255) || w[128 * 256 + 17] != value(x + 17, y + 128)) {
					bad++;
				}
			}
		}
		double s = (System.nanoTime() - t0) / 1e9;
		check("random windows: correct values", bad == 0);
		Map<String, Object> r = result("Random windows 256x256", n, s, "windows");
		r.put("MBps", n * 256.0 * 256 * 4 / 1048576 / s);
		print(r);
	}

	/** Several threads, each with its own dataset, read random windows at the same time. */
	static void threads(Path big) throws Exception {
		int per = (int) (600 * SCALE);
		List<Map<String, Object>> runs = new ArrayList<>();
		int cpus = Runtime.getRuntime().availableProcessors();
		for (int threads : new int[] { 1, 2, 4, Math.min(8, cpus), Math.min(16, cpus) }) {
			if (!runs.isEmpty() && ((Number) runs.get(runs.size() - 1).get("threads")).intValue() >= threads) {
				continue;
			}
			ExecutorService pool = Executors.newFixedThreadPool(threads);
			AtomicLong bad = new AtomicLong();
			long t0 = System.nanoTime();
			List<Future<?>> futures = new ArrayList<>();
			for (int t = 0; t < threads; t++) {
				int seed = t;
				futures.add(pool.submit(() -> {
					Random random = new Random(seed);
					try (GdalDataset ds = Gdal.open(big.toString())) {
						int size = ds.width();
						for (int i = 0; i < per; i++) {
							int x = random.nextInt(size - 256), y = random.nextInt(size - 256);
							float[] w = ds.band(1).readFloats(x, y, 256, 256);
							if (w[0] != value(x, y) || w[w.length - 1] != value(x + 255, y + 255)) {
								bad.incrementAndGet();
							}
						}
					}
					return null;
				}));
			}
			for (Future<?> f : futures) {
				f.get();
			}
			pool.shutdown();
			double s = (System.nanoTime() - t0) / 1e9;
			check("threads (" + threads + "): correct values", bad.get() == 0);
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("threads", threads);
			r.put("windows", threads * per);
			r.put("seconds", s);
			r.put("perSecond", threads * per / s);
			runs.add(r);
			System.out.printf(Locale.ROOT, "%-24s %2d threads: %6d windows in %5.2f s = %8.0f windows/s%n", "Threads", threads, threads * per, s, threads * per / s);
		}
		RESULTS.put("threads", runs);
	}

	/** Repeated reprojections (gdalwarp in memory). */
	static void warps(Path dem) {
		int n = (int) (150 * SCALE);
		long t0 = System.nanoTime();
		int w0 = -1, h0 = -1;
		boolean same = true;
		try (GdalDataset src = Gdal.open(dem.toString())) {
			for (int i = 0; i < n; i++) {
				String crs = i % 3 == 0 ? "EPSG:4326" : i % 3 == 1 ? "EPSG:3857" : "EPSG:25832";
				try (GdalDataset out = Gdal.warp(src, "", "-of", "MEM", "-t_srs", crs, "-r", "bilinear")) {
					if (i == 0) {
						w0 = out.width();
						h0 = out.height();
					} else if (i % 3 == 0) {
						same &= out.width() == w0 && out.height() == h0;
					}
				}
			}
		}
		double s = (System.nanoTime() - t0) / 1e9;
		check("warps: the same result size each time", same);
		print(result("Warp (gdalwarp, MEM)", n, s, "warps"));
	}

	/** Many vector features: write, read, attribute and spatial filters, SQL. */
	static void vectors(Path work) throws Exception {
		int n = (int) (100_000 * SCALE);
		Random random = new Random(3);
		double[] xs = new double[n], ys = new double[n];
		long sumValue = 0;
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("name", "Vectors");
		r.put("features", n);
		try (GdalDataset mem = Gdal.createVector("", "MEM"); SpatialReference utm = SpatialReference.fromEpsg(32632)) {
			VectorLayer layer = mem.createLayer("points", utm, GeometryType.POINT);
			layer.createField("id", FieldType.INTEGER);
			layer.createField("value", FieldType.INTEGER);
			layer.createField("name", FieldType.STRING);
			long t0 = System.nanoTime();
			for (int i = 0; i < n; i++) {
				xs[i] = 500000 + random.nextDouble() * 10000;
				ys[i] = 5290000 + random.nextDouble() * 10000;
				int v = random.nextInt(1000);
				sumValue += v;
				layer.addFeature(Map.of("id", i, "value", v, "name", "p" + (i % 100)), "POINT (" + xs[i] + " " + ys[i] + ")");
			}
			double write = (System.nanoTime() - t0) / 1e9;

			t0 = System.nanoTime();
			List<Feature> all = layer.features();
			double read = (System.nanoTime() - t0) / 1e9;
			long readSum = 0;
			for (Feature f : all) {
				readSum += ((Number) f.attributes().get("value")).longValue();
			}
			check("vectors: all features read with their values", all.size() == n && readSum == sumValue);

			/* Attribute filter, spatial filter (compared with a count in Java) */
			t0 = System.nanoTime();
			layer.setAttributeFilter("value < 100");
			long attr = layer.featureCount();
			layer.setAttributeFilter(null);
			layer.setSpatialFilter(502000, 5292000, 505000, 5296000);
			long spatial = layer.featureCount();
			layer.clearSpatialFilter();
			double filters = (System.nanoTime() - t0) / 1e9;
			long javaSpatial = 0;
			for (int i = 0; i < n; i++) {
				if (xs[i] >= 502000 && xs[i] <= 505000 && ys[i] >= 5292000 && ys[i] <= 5296000) {
					javaSpatial++;
				}
			}
			long javaAttr = all.stream().filter(f -> ((Number) f.attributes().get("value")).intValue() < 100).count();
			check("vectors: attribute filter count", attr == javaAttr);
			check("vectors: spatial filter count", spatial == javaSpatial);

			/* SQL: aggregates (OGR SQL) */
			t0 = System.nanoTime();
			long sqlCount = 0, sqlSum = 0;
			try (VectorLayer q = mem.executeSql("SELECT COUNT(*) AS n, SUM(value) AS s FROM points")) {
				Feature f = q.features().get(0);
				sqlCount = ((Number) f.attributes().get("n")).longValue();
				sqlSum = ((Number) f.attributes().get("s")).longValue();
			}
			double sql = (System.nanoTime() - t0) / 1e9;
			check("vectors: SQL count and sum", sqlCount == n && sqlSum == sumValue);

			r.put("writeSeconds", write);
			r.put("readSeconds", read);
			r.put("filterSeconds", filters);
			r.put("sqlSeconds", sql);
			r.put("writePerSecond", n / write);
			r.put("readPerSecond", n / read);
			System.out.printf(Locale.ROOT, "%-24s %d points: write %.1f s (%.0f/s), read %.1f s (%.0f/s), filters %.2f s, SQL %.2f s%n", "Vectors (MEM)", n, write, n / write,
					read, n / read, filters, sql);
		}

		/* A GeoPackage on disk: polygons */
		int m = (int) (20_000 * SCALE);
		Path gpkg = work.resolve("polygons.gpkg");
		long t0 = System.nanoTime();
		try (GdalDataset ds = Gdal.createVector(gpkg.toString(), "GPKG"); SpatialReference utm = SpatialReference.fromEpsg(32632)) {
			VectorLayer layer = ds.createLayer("cells", utm, GeometryType.POLYGON);
			layer.createField("cell", FieldType.INTEGER);
			int side = (int) Math.ceil(Math.sqrt(m));
			for (int i = 0; i < m; i++) {
				double x = 500000 + (i % side) * 10, y = 5290000 + (i / side) * 10;
				layer.addFeature(Map.of("cell", i), String.format(Locale.ROOT, "POLYGON ((%f %f, %f %f, %f %f, %f %f, %f %f))", x, y, x + 10, y, x + 10, y + 10, x, y + 10, x, y));
			}
		}
		double write = (System.nanoTime() - t0) / 1e9;
		t0 = System.nanoTime();
		long count;
		try (GdalDataset ds = Gdal.open(gpkg.toString())) {
			count = ds.layer(0).features().size();
		}
		double read = (System.nanoTime() - t0) / 1e9;
		check("GeoPackage: all polygons read", count == m);
		r.put("gpkgPolygons", m);
		r.put("gpkgWriteSeconds", write);
		r.put("gpkgReadSeconds", read);
		RESULTS.put("vectors", r);
		System.out.printf(Locale.ROOT, "%-24s %d polygons: write %.1f s (%.0f/s), read %.2f s%n", "Vectors (GeoPackage)", m, write, m / write, read);
	}

	/** A million coordinate transformations (in arrays) and single calls, with the round trip error. */
	static void transformations() {
		int n = (int) (1_000_000 * SCALE), single = (int) (50_000 * SCALE);
		Random random = new Random(5);
		double[] x = new double[n], y = new double[n], x0 = new double[n], y0 = new double[n];
		for (int i = 0; i < n; i++) {
			x0[i] = x[i] = 300000 + random.nextDouble() * 400000;
			y0[i] = y[i] = 5000000 + random.nextDouble() * 1000000;
		}
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("name", "Coordinate transformations");
		try (SpatialReference utm = SpatialReference.fromEpsg(32632);
				SpatialReference wgs = SpatialReference.fromEpsg(4326);
				CoordinateTransform there = utm.transformTo(wgs);
				CoordinateTransform back = wgs.transformTo(utm)) {
			long t0 = System.nanoTime();
			there.transform(x, y);
			back.transform(x, y);
			double s = (System.nanoTime() - t0) / 1e9;
			double max = 0;
			for (int i = 0; i < n; i++) {
				max = Math.max(max, Math.hypot(x[i] - x0[i], y[i] - y0[i]));
			}
			check("transformations: round trip error < 1 mm", max < 0.001);
			t0 = System.nanoTime();
			double sum = 0;
			for (int i = 0; i < single; i++) {
				sum += there.transform(x0[i], y0[i])[0];
			}
			double s1 = (System.nanoTime() - t0) / 1e9;
			check("transformations: single calls", sum > 0);
			r.put("points", n);
			r.put("arraySeconds", s);
			r.put("arrayPerSecond", 2.0 * n / s);
			r.put("roundTripMaxErrorM", max);
			r.put("singleCalls", single);
			r.put("singleSeconds", s1);
			r.put("singlePerSecond", single / s1);
			System.out.printf(Locale.ROOT, "%-24s %d points there and back in %.2f s (%.1f million/s), round trip error %.2e m; %d single calls: %.0f/s%n", "Transformations", n,
					s, 2.0 * n / s / 1e6, max, single, single / s1);
		}
		RESULTS.put("transformations", r);
	}

	/** Storms of errors: each must be a GdalException (no crash), afterwards GDAL still works. */
	static void errors(Path work, Path dem) throws Exception {
		int missing = (int) (2000 * SCALE), corrupt = (int) (300 * SCALE), sql = (int) (500 * SCALE), crs = (int) (300 * SCALE);
		Path bad = work.resolve("corrupt.tif");
		byte[] junk = new byte[4096];
		new Random(9).nextBytes(junk);
		junk[0] = 'I';
		junk[1] = 'I';
		junk[2] = 42;
		junk[3] = 0;   // a TIFF header, then garbage
		Files.write(bad, junk);
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("name", "Errors");
		long t0 = System.nanoTime();
		int caught = 0, other = 0;
		String example = null;
		for (int i = 0; i < missing; i++) {
			try (GdalDataset ds = Gdal.open(work.resolve("missing_" + i + ".tif").toString())) {
				/* not expected */
			} catch (GdalException e) {
				caught++;
				example = e.getMessage();
			} catch (RuntimeException e) {
				other++;
			}
		}
		r.put("missingFiles", missing);
		r.put("missingCaught", caught);
		r.put("missingMessage", example);
		check("errors: missing files", caught == missing);

		caught = 0;
		for (int i = 0; i < corrupt; i++) {
			try (GdalDataset ds = Gdal.open(bad.toString())) {
				ds.band(1).readFloats();
			} catch (GdalException e) {
				caught++;
				example = e.getMessage();
			} catch (RuntimeException e) {
				other++;
			}
		}
		r.put("corruptFiles", corrupt);
		r.put("corruptCaught", caught);
		r.put("corruptMessage", example);
		check("errors: corrupt files", caught == corrupt);

		caught = 0;
		try (GdalDataset mem = Gdal.createVector("", "MEM")) {
			mem.createLayer("t", null, GeometryType.POINT);
			for (int i = 0; i < sql; i++) {
				try (VectorLayer q = mem.executeSql("SELEKT nothing FROM nowhere_" + i)) {
					/* not expected */
				} catch (GdalException e) {
					caught++;
					example = e.getMessage();
				} catch (RuntimeException e) {
					other++;
				}
			}
		}
		r.put("badSql", sql);
		r.put("badSqlCaught", caught);
		r.put("badSqlMessage", example);
		check("errors: wrong SQL", caught == sql);

		caught = 0;
		for (int i = 0; i < crs; i++) {
			try (SpatialReference s = SpatialReference.fromEpsg(990000 + i)) {
				/* not expected */
			} catch (GdalException e) {
				caught++;
				example = e.getMessage();
			} catch (RuntimeException e) {
				other++;
			}
		}
		r.put("badCrs", crs);
		r.put("badCrsCaught", caught);
		r.put("badCrsMessage", example);
		check("errors: unknown coordinate systems", caught == crs);
		check("errors: only GdalExceptions", other == 0);

		/* GDAL still works after the errors */
		try (GdalDataset ds = Gdal.open(dem.toString())) {
			check("errors: GDAL works after the errors", ds.band(1).readFloats().length == ds.width() * ds.height());
		}
		double s = (System.nanoTime() - t0) / 1e9;
		r.put("seconds", s);
		r.put("otherExceptions", other);
		RESULTS.put("errors", r);
		System.out.printf(Locale.ROOT, "%-24s %d missing, %d corrupt files, %d wrong SQL, %d unknown CRS: all GdalException (%.2f s)%n", "Errors", missing, corrupt, sql, crs, s);
	}

	/** The same mixed work in rounds: the native and Java memory after each round (a leak grows). */
	static void memoryRounds(Path dem) {
		int rounds = 8;
		List<double[]> series = new ArrayList<>();   // round, RSS MB, heap MB, seconds
		long t0 = System.nanoTime();
		for (int round = 0; round <= rounds; round++) {
			if (round > 0) {
				try (GdalDataset src = Gdal.open(dem.toString())) {
					for (int i = 0; i < 300 * SCALE; i++) {
						try (GdalDataset ds = Gdal.open(dem.toString())) {
							ds.band(1).readFloats(0, 0, 100, 100);
						}
					}
					for (int i = 0; i < 10 * SCALE; i++) {
						try (GdalDataset out = Gdal.warp(src, "", "-of", "MEM", "-t_srs", "EPSG:4326")) {
							out.band(1).readFloats();
						}
					}
				}
				try (GdalDataset mem = Gdal.createVector("", "MEM")) {
					VectorLayer layer = mem.createLayer("p", null, GeometryType.POINT);
					layer.createField("v", FieldType.INTEGER);
					for (int i = 0; i < 5000 * SCALE; i++) {
						layer.addFeature(Map.of("v", i), "POINT (" + i + " " + i + ")");
					}
					layer.features();
				}
				try (SpatialReference a = SpatialReference.fromEpsg(32632); SpatialReference b = SpatialReference.fromEpsg(4326); CoordinateTransform ct = a.transformTo(b)) {
					double[] x = new double[(int) (100_000 * SCALE)], y = new double[x.length];
					java.util.Arrays.fill(x, 500000);
					java.util.Arrays.fill(y, 5300000);
					ct.transform(x, y);
				}
			}
			System.gc();
			Runtime rt = Runtime.getRuntime();
			series.add(new double[] { round, rssMB(), (rt.totalMemory() - rt.freeMemory()) / 1048576.0, (System.nanoTime() - t0) / 1e9 });
		}
		/* The growth per round (least squares of the rounds 2..n, round 1 fills the caches) */
		double slope = slope(series.subList(2, series.size()), 1);
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("name", "Memory rounds");
		r.put("rounds", rounds);
		r.put("series", series);
		r.put("rssGrowthMBPerRound", slope);
		r.put("heapGrowthMBPerRound", slope(series.subList(2, series.size()), 2));
		RESULTS.put("memory", r);
		check("memory: no leak (native memory grows < 1 MB per round)", Double.isNaN(slope) || slope < 1);
		System.out.printf(Locale.ROOT, "%-24s %d rounds: native memory %.0f -> %.0f MB, growth %.2f MB/round (heap %.2f MB/round)%n", "Memory", rounds, series.get(1)[1],
				series.get(series.size() - 1)[1], slope, r.get("heapGrowthMBPerRound"));
	}

	/** The compatibility layer (org.gdal, as used by WorldWind): open, read, delete. */
	static void compatibility(Path big) {
		int n = (int) (1000 * SCALE);
		long t0 = System.nanoTime();
		int bad = 0;
		for (int i = 0; i < n; i++) {
			org.gdal.gdal.Dataset ds = org.gdal.gdal.gdal.Open(big.toString());
			org.gdal.gdal.Band band = ds.GetRasterBand(1);
			int x = (i * 37) % 1000, y = (i * 53) % 1000;
			ByteBuffer buf = band.ReadRaster_Direct(x, y, 64, 64, org.gdal.gdalconst.gdalconst.GDT_Float32);
			buf.order(ByteOrder.nativeOrder());
			if (buf.getFloat(0) != value(x, y)) {
				bad++;
			}
			band.delete();
			ds.delete();
		}
		double s = (System.nanoTime() - t0) / 1e9;
		check("compatibility layer: correct values", bad == 0);
		print(result("Compatibility (org.gdal)", n, s, "open+read+delete"));
	}

	/** Contour lines and polygons of the elevation model, repeated. */
	static void terrainTools(Path dem) {
		int n = (int) (40 * SCALE);
		long t0 = System.nanoTime();
		long lines = -1;
		boolean same = true;
		try (GdalDataset src = Gdal.open(dem.toString())) {
			for (int i = 0; i < n; i++) {
				try (GdalDataset mem = Gdal.createVector("", "MEM")) {
					VectorLayer layer = mem.createLayer("c", src.spatialReference(), GeometryType.LINESTRING);
					src.band(1).contours(layer, 20);
					long c = layer.featureCount();
					same &= lines < 0 || c == lines;
					lines = c;
				}
			}
		}
		double s = (System.nanoTime() - t0) / 1e9;
		check("contours: the same result each time", same && lines > 0);
		Map<String, Object> r = result("Contours (20 m)", n, s, "runs");
		r.put("lines", lines);
		print(r);
	}

	/* ---------------- Helpers ---------------- */

	/** The value of a pixel of the large raster (exact in float). */
	static float value(int x, int y) {
		return ((x * 31 + y * 17) % 4096) * 0.25f;
	}

	private static int projectionLength(Path dem) {
		try (GdalDataset ds = Gdal.open(dem.toString())) {
			return ds.projection().length();
		}
	}

	private static Map<String, Object> result(String name, int n, double seconds, String unit) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("name", name);
		r.put("count", n);
		r.put("unit", unit);
		r.put("seconds", seconds);
		r.put("perSecond", n / seconds);
		r.put("rssMB", rssMB());
		RESULTS.put(name, r);
		return r;
	}

	private static void print(Map<String, Object> r) {
		System.out.printf(Locale.ROOT, "%-24s %7d %s in %6.2f s = %9.0f/s%n", r.get("name"), r.get("count"), r.get("unit"), r.get("seconds"), r.get("perSecond"));
	}

	private static void check(String what, boolean ok) {
		CHECKS.put(what, ok);
		if (!ok) {
			FAILURES.add(what);
			System.err.println("FAILED: " + what);
		}
	}

	/** The resident memory of the process (MB), NaN if unknown (Windows). */
	static double rssMB() {
		try {
			Process p = new ProcessBuilder("ps", "-o", "rss=", "-p", String.valueOf(ProcessHandle.current().pid())).start();
			String out = new String(p.getInputStream().readAllBytes()).trim();
			p.waitFor();
			return Long.parseLong(out) / 1024.0;
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	/** The slope of column c over column 0 (least squares). */
	private static double slope(List<double[]> s, int c) {
		double n = s.size(), sx = 0, sy = 0, sxx = 0, sxy = 0;
		for (double[] p : s) {
			sx += p[0];
			sy += p[c];
			sxx += p[0] * p[0];
			sxy += p[0] * p[c];
		}
		return (n * sxy - sx * sy) / (n * sxx - sx * sx);
	}

	/* A small JSON writer (maps, lists, numbers, strings, arrays of doubles) */
	@SuppressWarnings("unchecked")
	private static String toJson(Object o, String indent) {
		if (o == null) {
			return "null";
		}
		if (o instanceof Map<?, ?> m) {
			StringBuilder b = new StringBuilder("{\n");
			int i = 0;
			for (Map.Entry<?, ?> e : m.entrySet()) {
				b.append(indent).append("  \"").append(e.getKey()).append("\": ").append(toJson(e.getValue(), indent + "  ")).append(++i < m.size() ? ",\n" : "\n");
			}
			return b.append(indent).append('}').toString();
		}
		if (o instanceof List<?> l) {
			StringBuilder b = new StringBuilder("[");
			for (int i = 0; i < l.size(); i++) {
				b.append(i > 0 ? ", " : "").append(toJson(l.get(i), indent + "  "));
			}
			return b.append(']').toString();
		}
		if (o instanceof double[] d) {
			StringBuilder b = new StringBuilder("[");
			for (int i = 0; i < d.length; i++) {
				b.append(i > 0 ? ", " : "").append(toJson(d[i], indent));
			}
			return b.append(']').toString();
		}
		if (o instanceof Double d) {
			return d.isNaN() || d.isInfinite() ? "null" : String.format(Locale.ROOT, "%.6g", d);
		}
		if (o instanceof Number || o instanceof Boolean) {
			return o.toString();
		}
		return "\"" + o.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
	}
}
