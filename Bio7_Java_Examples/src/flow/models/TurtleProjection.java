package flow.models;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.eco.bio7.rbridge.RUtil;

/**
 * Step 2 of the flow Models/4_Sea_Turtle (Bio7_Flow_Examples): stochastic
 * projections of the stage matrix of the loggerhead sea turtle, which the first
 * step defined in R ("turtleMatrix", 7 stages after Crouse et al. 1987).
 *
 * Three scenarios of management (as in the study): no change, nest protection
 * (the survival of the eggs and hatchlings raised to 0.9) and turtle excluder
 * devices in shrimp trawls (the survival of large juveniles and subadults
 * raised by 15 %). Each year the survival of all stages varies (log-normal,
 * 10 %). 500 runs of 100 years from the stable stage distribution with 2000
 * turtles: the share of runs below 500 turtles (quasi-extinction) and the median
 * size per year, to R as the data frame "turtleScenarios".
 */
public class TurtleProjection {

	static final String[] SCENARIOS = { "No change", "Nest protection", "Turtle excluder devices" };

	public static void main(String[] args) throws Exception {
		double[][] base = RUtil.fromRMatrix("turtleMatrix");
		double[] start = RUtil.fromRDoubles("turtleStableStage");
		int stages = base.length, years = 100, runs = 500;
		double threshold = 500, initial = 2000, sigma = 0.1;
		int rows = SCENARIOS.length * (years + 1);
		String[] scenario = new String[rows];
		int[] year = new int[rows];
		double[] quasiExtinct = new double[rows], median = new double[rows];
		Random random = new Random(11);
		int row = 0;
		for (int s = 0; s < SCENARIOS.length; s++) {
			double[][] a = scenarioMatrix(base, s);
			int[] below = new int[years + 1];
			double[][] sizes = new double[years + 1][runs];
			boolean[] extinct = new boolean[runs];
			for (int run = 0; run < runs; run++) {
				if (Thread.currentThread().isInterrupted()) {
					System.out.println("Turtle projection cancelled.");
					return;
				}
				double[] v = new double[stages];
				for (int i = 0; i < stages; i++) {
					v[i] = start[i] * initial;
				}
				sizes[0][run] = initial;
				for (int t = 1; t <= years; t++) {
					/* The survival (rows 2 to 7) varies each year, the fecundities (row 1) not */
					double factor = Math.exp(sigma * random.nextGaussian() - sigma * sigma / 2);
					double[] w = new double[stages];
					for (int i = 0; i < stages; i++) {
						double sum = 0;
						for (int j = 0; j < stages; j++) {
							double aij = i == 0 ? a[i][j] : Math.min(1, a[i][j] * factor);
							sum += aij * v[j];
						}
						w[i] = sum;
					}
					v = w;
					/* The adults and juveniles (without the eggs) */
					double total = 0;
					for (int i = 1; i < stages; i++) {
						total += v[i];
					}
					sizes[t][run] = total;
					if (total < threshold) {
						extinct[run] = true;
					}
					if (extinct[run]) {
						below[t]++;
					}
				}
			}
			for (int t = 0; t <= years; t++) {
				scenario[row] = SCENARIOS[s];
				year[row] = t;
				quasiExtinct[row] = (double) below[t] / runs;
				double[] sorted = sizes[t].clone();
				Arrays.sort(sorted);
				median[row] = sorted[runs / 2];
				row++;
			}
			System.out.printf(java.util.Locale.ROOT, "%-24s quasi-extinction after %d years: %.2f%n", SCENARIOS[s], years, (double) below[years] / runs);
		}
		Map<String, Object> columns = new LinkedHashMap<>();
		columns.put("scenario", scenario);
		columns.put("year", year);
		columns.put("quasi_extinction", quasiExtinct);
		columns.put("median_size", median);
		RUtil.dataFrameToR("turtleScenarios", columns);
		System.out.println("Turtle projection: " + SCENARIOS.length + " scenarios in the R data frame 'turtleScenarios'.");
	}

	/* The matrix of a scenario (the columns: from stage j, the rows: to stage i) */
	static double[][] scenarioMatrix(double[][] base, int scenario) {
		double[][] a = new double[base.length][];
		for (int i = 0; i < base.length; i++) {
			a[i] = base[i].clone();
		}
		if (scenario == 1) {
			/* Nest protection: eggs and hatchlings survive to small juveniles with 0.9 */
			a[1][0] = 0.9;
		} else if (scenario == 2) {
			/* Turtle excluder devices: large juveniles (3) and subadults (4) survive better */
			for (int j = 2; j <= 3; j++) {
				double survival = 0;
				for (int i = 1; i < a.length; i++) {
					survival += a[i][j];
				}
				double f = Math.min(1.15, 0.98 / survival);
				for (int i = 1; i < a.length; i++) {
					a[i][j] *= f;
				}
			}
		}
		return a;
	}
}
