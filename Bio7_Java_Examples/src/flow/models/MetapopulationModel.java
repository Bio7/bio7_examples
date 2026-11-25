package flow.models;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.eco.bio7.pybridge.PythonUtil;
import com.eco.bio7.rbridge.RUtil;

/**
 * Step 2 of the flow Models/3_Metapopulation (Bio7_Flow_Examples): the
 * incidence function model (Hanski 1994) on the patches of the landscape the
 * first step created in Python (the pandas DataFrame "patches": x, y in km,
 * area in ha).
 *
 * <pre>
 * connectivity  S_i = sum_j occupied  exp(-alpha d_ij) A_j^b
 * colonisation  C_i = S_i^2 / (S_i^2 + y^2)
 * extinction    E_i = min(1, e / A_i^x)
 * </pre>
 *
 * 20 runs of 200 years from half of the patches occupied: the share of the last
 * 100 years each patch was occupied and its mean connectivity (to R: the data
 * frame "patchOccupancy") and the share of occupied patches per year
 * ("occupancyTime").
 */
public class MetapopulationModel {

	public static void main(String[] args) throws Exception {
		double[] x = PythonUtil.fromPythonDoubles("patches['x'].to_numpy()");
		double[] y = PythonUtil.fromPythonDoubles("patches['y'].to_numpy()");
		double[] area = PythonUtil.fromPythonDoubles("patches['area'].to_numpy()");
		int n = x.length;
		double alpha = 1.0, b = 0.5, yc = 2.0, e = 0.2, xe = 0.8;
		int years = 200, runs = 20, recordFrom = 100;
		double[][] dispersal = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i != j) {
					double d = Math.hypot(x[i] - x[j], y[i] - y[j]);
					dispersal[i][j] = Math.exp(-alpha * d) * Math.pow(area[j], b);
				}
			}
		}
		double[] occupiedYears = new double[n], connectivity = new double[n];
		int[] runOf = new int[runs * years], yearOf = new int[runs * years];
		double[] share = new double[runs * years];
		Random random = new Random(7);
		int row = 0, recorded = 0;
		for (int run = 1; run <= runs; run++) {
			boolean[] occ = new boolean[n];
			for (int i = 0; i < n; i++) {
				occ[i] = random.nextDouble() < 0.5;
			}
			for (int t = 1; t <= years; t++) {
				if (Thread.currentThread().isInterrupted()) {
					System.out.println("Metapopulation model cancelled.");
					return;
				}
				boolean[] next = new boolean[n];
				for (int i = 0; i < n; i++) {
					double s = 0;
					for (int j = 0; j < n; j++) {
						if (occ[j]) {
							s += dispersal[i][j];
						}
					}
					if (t > recordFrom) {
						connectivity[i] += s;
					}
					if (occ[i]) {
						double ext = Math.min(1, e / Math.pow(area[i], xe));
						next[i] = random.nextDouble() >= ext;
					} else {
						double col = s * s / (s * s + yc * yc);
						next[i] = random.nextDouble() < col;
					}
				}
				occ = next;
				int count = 0;
				for (int i = 0; i < n; i++) {
					if (occ[i]) {
						count++;
						if (t > recordFrom) {
							occupiedYears[i]++;
						}
					}
				}
				runOf[row] = run;
				yearOf[row] = t;
				share[row] = (double) count / n;
				row++;
			}
			recorded += years - recordFrom;
		}
		double[] incidence = new double[n];
		for (int i = 0; i < n; i++) {
			incidence[i] = occupiedYears[i] / recorded;
			connectivity[i] /= recorded;
		}
		int[] occupied = new int[n], empty = new int[n];
		for (int i = 0; i < n; i++) {
			occupied[i] = (int) occupiedYears[i];
			empty[i] = recorded - occupied[i];
		}
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("x", x);
		p.put("y", y);
		p.put("area", area);
		p.put("connectivity", connectivity);
		p.put("incidence", incidence);
		p.put("occupied", occupied);
		p.put("empty", empty);
		RUtil.dataFrameToR("patchOccupancy", p);
		Map<String, Object> ts = new LinkedHashMap<>();
		ts.put("run", runOf);
		ts.put("year", yearOf);
		ts.put("share", share);
		RUtil.dataFrameToR("occupancyTime", ts);
		System.out.println("Metapopulation model: " + n + " patches, " + runs + " runs of " + years + " years in the R data frames 'patchOccupancy' and 'occupancyTime'.");
	}
}
