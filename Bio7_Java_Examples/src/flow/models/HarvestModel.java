package flow.models;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.eco.bio7.rbridge.RUtil;

/**
 * Step 1 of the flow Models/1_Harvest (Bio7_Flow_Examples): a fish population
 * with logistic growth and environmental noise, harvested with a constant rate
 * (the fraction caught each year).
 *
 * <pre>
 * N(t+1) = (N + r N (1 - N/K)) * exp(sigma * z - sigma^2/2) - h * N
 * </pre>
 *
 * For each harvest rate h (0 to 0.6) 30 runs of 50 years: the total catch and
 * whether the stock collapsed (below 10 fish). The runs go to R as the data
 * frame "harvest" (the next step of the flow analyses them). Without noise the
 * maximum sustainable yield is at h = r/2.
 */
public class HarvestModel {

	public static void main(String[] args) throws Exception {
		double r = 0.8, k = 1000, sigma = 0.25;
		int years = 50, runs = 30;
		int rates = 25;
		int n = rates * runs;
		double[] rate = new double[n], catchTotal = new double[n], stock = new double[n];
		int[] run = new int[n];
		boolean[] collapsed = new boolean[n];
		Random random = new Random(42);
		int row = 0;
		for (int i = 0; i < rates; i++) {
			double h = i * 0.025;
			for (int rep = 1; rep <= runs; rep++) {
				/* The flow can be cancelled (the thread is interrupted) */
				if (Thread.currentThread().isInterrupted()) {
					System.out.println("Harvest model cancelled.");
					return;
				}
				double size = k / 2, total = 0;
				boolean collapse = false;
				for (int t = 0; t < years && !collapse; t++) {
					size = (size + r * size * (1 - size / k)) * Math.exp(sigma * random.nextGaussian() - sigma * sigma / 2);
					double caught = h * size;
					size -= caught;
					total += caught;
					if (size < 10) {
						collapse = true;
						size = 0;
					}
				}
				rate[row] = h;
				run[row] = rep;
				catchTotal[row] = total;
				stock[row] = size;
				collapsed[row] = collapse;
				row++;
			}
		}
		Map<String, Object> columns = new LinkedHashMap<>();
		columns.put("rate", rate);
		columns.put("run", run);
		columns.put("catch", catchTotal);
		columns.put("stock", stock);
		columns.put("collapsed", collapsed);
		RUtil.dataFrameToR("harvest", columns);
		System.out.println("Harvest model: " + n + " runs (" + rates + " harvest rates x " + runs + " runs) in the R data frame 'harvest'.");
	}
}
