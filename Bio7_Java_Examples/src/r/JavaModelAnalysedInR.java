package r;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.eco.bio7.rbridge.RUtil;

/*
 * A model in Java (random walks of animals), the positions to the embedded R
 * (the FFM bridge) as data frame, analysed in R (the distances, a 2-d kernel
 * density) and the results back to Java: the density matrix of R becomes an
 * ImageJ image.
 * Start R first (embedded R, the R button of the toolbar). Uses the R package
 * MASS (a recommended package of R).
 */
public class JavaModelAnalysedInR {

	public static void main(String[] args) throws Exception {
		if (!RUtil.start()) {
			System.out.println("R could not be started (see the console)!");
			return;
		}

		/* ---------- The model (Java) ---------- */
		int animals = 20, steps = 200;
		int n = animals * steps;
		int[] id = new int[n];
		int[] step = new int[n];
		double[] x = new double[n];
		double[] y = new double[n];
		Random random = new Random(3);
		int i = 0;
		for (int a = 0; a < animals; a++) {
			double px = 50, py = 50, heading = random.nextDouble() * 2 * Math.PI;
			for (int s = 0; s < steps; s++) {
				/* A correlated random walk in a square of 100 x 100 (reflecting borders) */
				heading += random.nextGaussian() * 0.5;
				px += Math.cos(heading);
				py += Math.sin(heading);
				if (px < 0 || px > 100) {
					heading = Math.PI - heading;
					px = Math.max(0, Math.min(100, px));
				}
				if (py < 0 || py > 100) {
					heading = -heading;
					py = Math.max(0, Math.min(100, py));
				}
				id[i] = a + 1;
				step[i] = s + 1;
				x[i] = px;
				y[i] = py;
				i++;
			}
		}

		/* ---------- Java -> R: a data frame ---------- */
		Map<String, Object> walks = new LinkedHashMap<>();
		walks.put("id", id);
		walks.put("step", step);
		walks.put("x", x);
		walks.put("y", y);
		RUtil.dataFrameToR("walks", walks);

		/* ---------- The analysis (R) ---------- */
		RUtil.exec("""
				walks$dist <- with(walks, sqrt((x - 50)^2 + (y - 50)^2))
				final <- walks[walks$step == max(walks$step), ]
				net <- tapply(walks$dist, walks$id, function(d) d[length(d)])
				# The density (rows = y from the top as in an image)
				k <- MASS::kde2d(walks$x, walks$y, n = 200, lims = c(0, 100, 0, 100))
				density <- t(k$z)[200:1, ]
				""");

		/* ---------- R -> Java ---------- */
		double[] net = RUtil.fromRDoubles("as.vector(net)");
		System.out.printf(java.util.Locale.ROOT, "Mean distance from the start after %d steps: %.2f%n", steps, RUtil.fromRDouble("mean(final$dist)"));
		System.out.println("Net distance of the animals: " + Arrays.toString(Arrays.stream(net).map(d -> Math.round(d * 10) / 10.0).toArray()));
		System.out.println(RUtil.print("summary(final$dist)"));

		/* The density matrix of R as ImageJ image */
		RUtil.imageFromR("density", "Density of the walks (R)");
	}
}
