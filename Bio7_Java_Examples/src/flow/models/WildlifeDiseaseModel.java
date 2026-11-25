package flow.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.eco.bio7.rbridge.RUtil;

/**
 * Step 1 of the flow Models/5_Wildlife_Disease (Bio7_Flow_Examples): a disease
 * in a population of 500 animals (e.g., wild boar), an SIR model simulated with
 * the Gillespie algorithm (exact stochastic events).
 *
 * <pre>
 * infection  S + I -> 2 I   rate beta S I / N   (beta = 0.4 per day)
 * recovery   I -> R         rate gamma I        (gamma = 1/7 per day)
 * </pre>
 *
 * A share of the population is vaccinated (immune) before 3 animals bring in
 * the disease: coverages from 0 to 0.8, 100 runs each. To R: the final size,
 * the peak and the duration of each run (the data frame "epidemics") and the
 * infected per day of the first 20 runs of each coverage ("epidemicCurves").
 * The basic reproduction number is beta / gamma = 2.8.
 */
public class WildlifeDiseaseModel {

	public static void main(String[] args) throws Exception {
		int population = 500, introduced = 3, runs = 100, curves = 20, maxDays = 365;
		double beta = 0.4, gamma = 1.0 / 7;
		double[] coverages = { 0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8 };
		int n = coverages.length * runs;
		double[] coverage = new double[n], finalSize = new double[n], peak = new double[n], duration = new double[n];
		int[] run = new int[n];
		List<Double> cCov = new ArrayList<>(), cDay = new ArrayList<>(), cInf = new ArrayList<>();
		List<Integer> cRun = new ArrayList<>();
		Random random = new Random(3);
		int row = 0;
		for (double v : coverages) {
			for (int rep = 1; rep <= runs; rep++) {
				if (Thread.currentThread().isInterrupted()) {
					System.out.println("Disease model cancelled.");
					return;
				}
				int vaccinated = (int) Math.round(v * population);
				int s = population - vaccinated - introduced, i = introduced, r = vaccinated;
				double t = 0;
				int maxI = i, nextDay = 0;
				boolean keep = rep <= curves;
				while (i > 0 && t < maxDays) {
					double infection = beta * s * i / population;
					double recovery = gamma * i;
					double total = infection + recovery;
					double dt = -Math.log(1 - random.nextDouble()) / total;
					/* The infected at each full day before the event */
					while (keep && nextDay <= t + dt && nextDay <= maxDays) {
						cCov.add(v);
						cRun.add(rep);
						cDay.add((double) nextDay);
						cInf.add((double) i);
						nextDay++;
					}
					t += dt;
					if (random.nextDouble() * total < infection) {
						s--;
						i++;
					} else {
						i--;
						r++;
					}
					maxI = Math.max(maxI, i);
				}
				coverage[row] = v;
				run[row] = rep;
				/* The animals infected (the recovered without the vaccinated) */
				finalSize[row] = (double) (r - vaccinated) / population;
				peak[row] = maxI;
				duration[row] = t;
				row++;
			}
		}
		Map<String, Object> e = new LinkedHashMap<>();
		e.put("coverage", coverage);
		e.put("run", run);
		e.put("final_size", finalSize);
		e.put("peak", peak);
		e.put("duration", duration);
		RUtil.dataFrameToR("epidemics", e);
		Map<String, Object> c = new LinkedHashMap<>();
		c.put("coverage", toDoubles(cCov));
		c.put("run", cRun.stream().mapToInt(Integer::intValue).toArray());
		c.put("day", toDoubles(cDay));
		c.put("infected", toDoubles(cInf));
		RUtil.dataFrameToR("epidemicCurves", c);
		RUtil.toR("diseaseParameters", beta, gamma, population);
		RUtil.exec("names(diseaseParameters) <- c('beta', 'gamma', 'N')");
		System.out.println("Disease model: " + n + " runs in the R data frames 'epidemics' and 'epidemicCurves'.");
	}

	private static double[] toDoubles(List<Double> list) {
		double[] a = new double[list.size()];
		for (int k = 0; k < a.length; k++) {
			a[k] = list.get(k);
		}
		return a;
	}
}
