package geo.gdal.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.geo.GdalImageJ;
import com.eco.bio7.geo.GdalWorldWind;

import geo.gdal.GdalSetup;
import gui.swt.BackgroundJob;
import ij.ImagePlus;

/*
 * A raster tool with GDAL: the information of a raster file (gdalinfo), the
 * raster reprojected (gdalwarp: coordinate system, pixel size, resampling) and
 * converted (gdal_translate: GeoTIFF, COG, PNG, JPEG, ...), the result opened in
 * ImageJ (with its georeference) or shown in WorldWind.
 *
 * The GUI is in RasterToolGui (WindowBuilder: Open With > WindowBuilder Editor).
 */
public class RasterTool extends com.eco.bio7.compile.Model {

	private RasterToolGui gui;

	public RasterTool() {
		if (!GdalSetup.load()) {
			return;
		}
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Raster Tool");
			parent.setLayout(new FillLayout());
			gui = new RasterToolGui(parent, RasterTool.this, SWT.NONE);
			parent.layout(true);
			/* An example file (of Gdal01_RasterBasics) */
			try {
				Path dem = GdalSetup.folder().resolve("dem_utm.tif");
				if (Files.exists(dem)) {
					gui.setSource(dem.toString());
				}
			} catch (Exception e) {
				/* no example file */
			}
		});
	}

	public void info(String file) {
		gui.setBusy(true);
		BackgroundJob.run("gdalinfo", () -> {
			String info = Gdal.info(file, "-stats");
			Display.getDefault().asyncExec(() -> gui.setInfo(info));
			return "gdalinfo " + Path.of(file).getFileName();
		}, this::done);
	}

	/**
	 * gdalwarp (if a coordinate system or a pixel size is given) and gdal_translate
	 * into the format. format: a GDAL driver (GTiff, COG, PNG, JPEG, ...).
	 */
	public void convert(String file, String crs, String pixelSize, String resampling, String format, String output) {
		gui.setBusy(true);
		BackgroundJob.run("Raster conversion", () -> {
			Path out = Path.of(output);
			Files.deleteIfExists(out);
			String source = file;
			List<String> warp = new ArrayList<>(List.of("-of", "VRT", "-r", resampling, "-overwrite"));
			if (!crs.isBlank()) {
				warp.addAll(List.of("-t_srs", crs.trim()));
			}
			if (!pixelSize.isBlank()) {
				String s = pixelSize.trim().replace(',', '.');
				warp.addAll(List.of("-tr", s, s));
			}
			if (warp.size() > 5) {
				/* The reprojection as virtual raster (no file of its own), then written in the format */
				Path vrt = out.resolveSibling(out.getFileName() + ".warp.vrt");
				Gdal.warp(file, vrt.toString(), warp.toArray(String[]::new));
				source = vrt.toString();
			}
			List<String> opts = new ArrayList<>(List.of("-of", format));
			if (format.equals("PNG") || format.equals("JPEG")) {
				/* 8 bit for the image formats: scaled from the minimum to the maximum */
				opts.addAll(List.of("-ot", "Byte", "-scale"));
			} else if (format.equals("GTiff") || format.equals("COG")) {
				opts.addAll(List.of("-co", "COMPRESS=DEFLATE"));
			}
			Gdal.translate(source, out.toString(), opts.toArray(String[]::new));
			String info = Gdal.info(out.toString());
			Display.getDefault().asyncExec(() -> gui.setInfo(info));
			return "Written: " + out;
		}, this::done);
	}

	public void openInImageJ(String file) {
		gui.setBusy(true);
		BackgroundJob.run("Open in ImageJ", () -> {
			ImagePlus imp = GdalImageJ.open(Path.of(file));
			Display.getDefault().syncExec(imp::show);
			return "Opened in ImageJ: " + imp.getTitle() + " (the georeference is in Image > Show Info)";
		}, this::done);
	}

	public void showInWorldWind(String file) {
		gui.setBusy(true);
		BackgroundJob.run("WorldWind", () -> {
			GdalWorldWind.addRaster(Path.of(file));
			return "Shown in WorldWind: " + Path.of(file).getFileName();
		}, this::done);
	}

	private void done(String status) {
		if (!gui.isDisposed()) {
			gui.setStatus(status);
			gui.setBusy(false);
		}
	}
}
