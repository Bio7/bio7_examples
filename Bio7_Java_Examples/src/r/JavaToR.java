package r;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.eco.bio7.rbridge.RUtil;
import com.eco.bio7.rffm.RValue;

/*
 * Transfers data from Java to the embedded R of Bio7 (the FFM bridge) and the
 * results back to Java: vectors, strings, a matrix, a data frame and a list.
 * Plain Java types (double[], int[], String[], boolean[], double[][]).
 * Start R first (embedded R, the R button of the toolbar).
 */
public class JavaToR {

	public static void main(String[] args) throws Exception {
		if (!RUtil.isAlive()) {
			System.out.println("The embedded R is not running: please start R first!");
			return;
		}

		/* ---------- Java -> R ---------- */

		/* Numeric, integer, character and logical vectors */
		double[] temperature = { 12.1, 14.3, 13.8, 17.2, 19.5, 21.0, 18.4 };
		RUtil.toR("temperature", temperature);
		RUtil.toR("counts", 4, 8, 15, 16, 23, 42);
		RUtil.toR("species", "Daphnia", "Bosmina", "Cyclops");
		RUtil.toR("site", "Lake Constance");

		/* A matrix (the rows of Java) */
		RUtil.toR("m", new double[][] { { 1, 2, 3 }, { 4, 5, 6 } });

		/* A data frame: the columns in order (LinkedHashMap) */
		Map<String, Object> weather = new LinkedHashMap<>();
		weather.put("day", new int[] { 1, 2, 3, 4, 5, 6, 7 });
		weather.put("temperature", temperature);
		weather.put("sunny", new boolean[] { false, true, true, false, true, true, false });
		RUtil.dataFrameToR("weather", weather);

		/* A list with names */
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("depth", 2.5);
		settings.put("method", "net");
		RUtil.listToR("settings", settings);

		/* ---------- Computed in R ---------- */

		RUtil.exec("""
				fit <- lm(temperature ~ day, data = weather)
				m2 <- m %*% t(m)
				label <- sprintf("%s: %d species, depth %.1f m", site, length(species), settings$depth)
				result <- list(slope = coef(fit)[["day"]], r2 = summary(fit)$r.squared)
				warm <- subset(weather, temperature > 15)
				""");

		/* ---------- R -> Java ---------- */

		/* A numeric vector */
		double[] scaled = RUtil.fromRDoubles("as.vector(scale(temperature))");
		System.out.println("Scaled temperature: " + Arrays.toString(scaled));

		/* A scalar */
		System.out.println("Sum of the counts: " + RUtil.fromRDouble("sum(counts)"));

		/* A matrix (rows) */
		System.out.println("m %*% t(m): " + Arrays.deepToString(RUtil.fromRMatrix("m2")));

		/* A string */
		System.out.println(RUtil.fromRString("label"));

		/* A list: the elements by name */
		Map<String, RValue> result = RUtil.fromRList("result");
		System.out.println("Slope: " + RUtil.doubles(result.get("slope"))[0] + ", R²: " + RUtil.doubles(result.get("r2"))[0]);

		/* A data frame: the columns by name */
		Map<String, RValue> warm = RUtil.fromRList("warm");
		double[] days = RUtil.doubles(warm.get("day"));
		double[] temps = RUtil.doubles(warm.get("temperature"));
		for (int i = 0; i < days.length; i++) {
			System.out.println("Warm day " + (int) days[i] + ": " + temps[i] + " °C");
		}

		/* The printed output of R */
		System.out.println(RUtil.print("summary(fit)"));
	}
}
