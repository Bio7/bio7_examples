package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * A wireframe plot: the sombrero function z = sin(r) / r in one color.
 */
public class WireframePlotApi {

	public static void main(String[] args) {
		int n = 61;
		double[] x = new double[n], y = new double[n];
		double[][] z = new double[n][n];
		for (int i = 0; i < n; i++) {
			x[i] = -10 + 20.0 * i / (n - 1);
			y[i] = x[i];
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				double r = Math.hypot(x[i], y[j]);
				z[i][j] = r == 0 ? 1 : Math.sin(r) / r;
			}
		}
		new Plot3D().title("Sombrero\nz = sin(r) / r").axisTitles("x", "y", "z")
				.add(Plot3D.wireframe(x, y, z).step(1).color(0x1F4E9A).label("sin(r) / r"))
				.aspect(1, 1, 0.5)
				.show();
	}
}
