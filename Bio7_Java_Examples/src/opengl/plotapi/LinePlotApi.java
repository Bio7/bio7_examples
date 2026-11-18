package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A 3D line plot: the trajectory of the Lorenz attractor (colored by the
 * height) and the starting point as a sphere.
 */
public class LinePlotApi {

	public static void main(String[] args) {
		int n = 12000;
		double dt = 0.005, sigma = 10, rho = 28, beta = 8.0 / 3;
		double[] x = new double[n], y = new double[n], z = new double[n];
		x[0] = 1;
		y[0] = 1;
		z[0] = 1;
		for (int i = 1; i < n; i++) {
			double dx = sigma * (y[i - 1] - x[i - 1]);
			double dy = x[i - 1] * (rho - z[i - 1]) - y[i - 1];
			double dz = x[i - 1] * y[i - 1] - beta * z[i - 1];
			x[i] = x[i - 1] + dt * dx;
			y[i] = y[i - 1] + dt * dy;
			z[i] = z[i - 1] + dt * dz;
		}
		new Plot3D().title("Lorenz Attractor\nσ = 10, ρ = 28, β = 8/3").axisTitles("x", "y", "z")
				.add(Plot3D.line(x, y, z).colorMap(PlotLegend.VIRIDIS))
				.add(Plot3D.scatter(new double[] { x[0] }, new double[] { y[0] }, new double[] { z[0] }).color(0xD62728).size(0.25).label("Start"))
				.aspect(1, 1, 1)
				.show();
	}
}
