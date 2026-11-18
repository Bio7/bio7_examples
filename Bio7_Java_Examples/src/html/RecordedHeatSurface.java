package html;

import com.eco.bio7.spatial.HtmlSimulation;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SimulationRecorder;

/**
 * A dynamic surface computed in Java and recorded with SimulationRecorder: the
 * diffusion of heat from two pulsing sources on a plate. The page plays the
 * recorded surfaces (a 3D view with WebGL) with the chart of the mean
 * temperature.
 */
public class RecordedHeatSurface {

	public static SimulationRecorder record(int steps) {
		int n = 70;
		double[][] u = new double[n][n];
		SimulationRecorder recorder = new SimulationRecorder("Heat Diffusion (recorded in Java)")
				.description("Two pulsing heat sources on a plate with cold borders.").view(HtmlSimulation.View.SURFACE_3D)
				.colorMap(PlotLegend.RAINBOW).range(0, 3).box(1, 1, 0.4).chart("Temperature").speed(30);
		for (int t = 0; t < steps; t++) {
			double[][] next = new double[n][n];
			double sum = 0;
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					double lap = (i > 0 ? u[i - 1][j] : 0) + (i < n - 1 ? u[i + 1][j] : 0) + (j > 0 ? u[i][j - 1] : 0) + (j < n - 1 ? u[i][j + 1] : 0)
							- 4 * u[i][j];
					next[i][j] = u[i][j] + 0.2 * lap - 0.004 * u[i][j];
				}
			}
			/* The sources (small discs) pulse with different periods */
			heat(next, n / 3, n / 3, 0.35 * Math.max(0, Math.sin(t * 0.05)));
			heat(next, 2 * n / 3, 2 * n / 3, 0.35 * Math.max(0, Math.sin(t * 0.08 + 1)));
			u = next;
			for (double[] row : u) {
				for (double v : row) {
					sum += v;
				}
			}
			recorder.frame().surface(u).chart("Mean", sum / (n * n)).chart("Center", u[n / 2][n / 2]);
		}
		return recorder;
	}

	private static void heat(double[][] u, int ci, int cj, double power) {
		for (int i = ci - 3; i <= ci + 3; i++) {
			for (int j = cj - 3; j <= cj + 3; j++) {
				if ((i - ci) * (i - ci) + (j - cj) * (j - cj) <= 9) {
					u[i][j] += power;
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/RecordedHeatSurface.html";
		record(600).export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
