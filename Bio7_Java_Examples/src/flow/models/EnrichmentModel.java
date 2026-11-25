package flow.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eco.bio7.rbridge.RUtil;

/**
 * Step 1 of the flow Models/2_Enrichment (Bio7_Flow_Examples): the
 * predator-prey model of Rosenzweig and MacArthur (logistic prey, a predator
 * with a saturating functional response), integrated with Runge-Kutta (4th
 * order) for carrying capacities K from 0.5 to 6.
 *
 * <pre>
 * dN/dt = r N (1 - N/K) - a N P / (1 + a h N)
 * dP/dt = e a N P / (1 + a h N) - m P
 * </pre>
 *
 * The "paradox of enrichment" (Rosenzweig 1971): above a K the equilibrium
 * becomes unstable and the populations cycle with growing amplitude. To R: the
 * minimum and maximum of both populations after the transient for each K (the
 * data frame "enrichment") and two trajectories of the first 300 time units
 * (K = 1.5: a spiral into the equilibrium, K = 4: into a limit cycle,
 * "trajectories").
 */
public class EnrichmentModel {

	static final double R = 1.0, A = 1.0, H = 1.0, E = 0.5, M = 0.2;

	public static void main(String[] args) throws Exception {
		double dt = 0.02, transient_ = 1500, record = 500;
		List<Double> kList = new ArrayList<>();
		for (double k = 0.5; k <= 6.0001; k += 0.1) {
			kList.add(Math.round(k * 10) / 10.0);
		}
		int nk = kList.size();
		double[] ks = new double[nk], preyMin = new double[nk], preyMax = new double[nk], predMin = new double[nk], predMax = new double[nk];
		List<Double> tk = new ArrayList<>(), tt = new ArrayList<>(), tn = new ArrayList<>(), tp = new ArrayList<>();
		for (int i = 0; i < nk; i++) {
			double k = kList.get(i);
			double[] y = { 0.5, 0.2 };
			double nMin = Double.MAX_VALUE, nMax = 0, pMin = Double.MAX_VALUE, pMax = 0;
			boolean keep = Math.abs(k - 1.5) < 1e-9 || Math.abs(k - 4.0) < 1e-9;
			int steps = (int) Math.round((transient_ + record) / dt);
			for (int s = 0; s < steps; s++) {
				if (s % 10000 == 0 && Thread.currentThread().isInterrupted()) {
					System.out.println("Enrichment model cancelled.");
					return;
				}
				y = rk4(y, k, dt);
				double t = s * dt;
				/* The trajectories from the start: every 0.5 time units */
				if (keep && t <= 300 && s % 25 == 0) {
					tk.add(k);
					tt.add(t);
					tn.add(y[0]);
					tp.add(y[1]);
				}
				if (t >= transient_) {
					nMin = Math.min(nMin, y[0]);
					nMax = Math.max(nMax, y[0]);
					pMin = Math.min(pMin, y[1]);
					pMax = Math.max(pMax, y[1]);
				}
			}
			ks[i] = k;
			preyMin[i] = nMin;
			preyMax[i] = nMax;
			predMin[i] = pMin;
			predMax[i] = pMax;
		}
		Map<String, Object> e = new LinkedHashMap<>();
		e.put("K", ks);
		e.put("prey_min", preyMin);
		e.put("prey_max", preyMax);
		e.put("predator_min", predMin);
		e.put("predator_max", predMax);
		RUtil.dataFrameToR("enrichment", e);
		Map<String, Object> tr = new LinkedHashMap<>();
		tr.put("K", toArray(tk));
		tr.put("time", toArray(tt));
		tr.put("prey", toArray(tn));
		tr.put("predator", toArray(tp));
		RUtil.dataFrameToR("trajectories", tr);
		/* The parameters for the analysis in R */
		RUtil.toR("enrichmentParameters", R, A, H, E, M);
		RUtil.exec("names(enrichmentParameters) <- c('r', 'a', 'h', 'e', 'm')");
		System.out.println("Enrichment model: " + nk + " carrying capacities in the R data frames 'enrichment' and 'trajectories'.");
	}

	private static double[] derivative(double[] y, double k) {
		double n = y[0], p = y[1];
		double eaten = A * n * p / (1 + A * H * n);
		return new double[] { R * n * (1 - n / k) - eaten, E * eaten - M * p };
	}

	private static double[] rk4(double[] y, double k, double dt) {
		double[] k1 = derivative(y, k);
		double[] k2 = derivative(new double[] { y[0] + dt / 2 * k1[0], y[1] + dt / 2 * k1[1] }, k);
		double[] k3 = derivative(new double[] { y[0] + dt / 2 * k2[0], y[1] + dt / 2 * k2[1] }, k);
		double[] k4 = derivative(new double[] { y[0] + dt * k3[0], y[1] + dt * k3[1] }, k);
		return new double[] { Math.max(0, y[0] + dt / 6 * (k1[0] + 2 * k2[0] + 2 * k3[0] + k4[0])),
				Math.max(0, y[1] + dt / 6 * (k1[1] + 2 * k2[1] + 2 * k3[1] + k4[1])) };
	}

	private static double[] toArray(List<Double> list) {
		double[] a = new double[list.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = list.get(i);
		}
		return a;
	}
}
