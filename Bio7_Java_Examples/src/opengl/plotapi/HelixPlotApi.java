package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * A double helix (like DNA): two lines for the strands, the base pairs as lines
 * between the strands and the atoms of the backbone as spheres. In Blender the
 * lines are tubes and the points spheres (a model for a rendering). The export
 * action of the 3D view is enabled (HTML or Blender).
 */
public class HelixPlotApi {

	public static void main(String[] args) {
		int n = 400, pairs = 40;
		double turns = 4, radius = 1;
		double[] x1 = new double[n], y1 = new double[n], z1 = new double[n], x2 = new double[n], y2 = new double[n], z2 = new double[n];
		for (int i = 0; i < n; i++) {
			double t = 2 * Math.PI * turns * i / (n - 1), z = 12.0 * i / (n - 1);
			x1[i] = radius * Math.cos(t);
			y1[i] = radius * Math.sin(t);
			x2[i] = radius * Math.cos(t + Math.PI * 0.8);
			y2[i] = radius * Math.sin(t + Math.PI * 0.8);
			z1[i] = z;
			z2[i] = z;
		}
		Plot3D plot = new Plot3D().title("Double Helix").axisTitles("x", "y", "z").aspect(0.35, 0.35, 1).grids(false, false, false)
				.add(Plot3D.line(x1, y1, z1).color(0x1F77B4).label("Strand 1"))
				.add(Plot3D.line(x2, y2, z2).color(0xD62728).label("Strand 2"));
		/* The base pairs: a line from strand 1 to strand 2 and the atoms as spheres */
		double[] px = new double[pairs * 2], py = new double[pairs * 2], pz = new double[pairs * 2];
		for (int k = 0; k < pairs; k++) {
			int i = (int) Math.round((n - 1) * (k + 0.5) / pairs);
			plot.add(Plot3D.line(new double[] { x1[i], x2[i] }, new double[] { y1[i], y2[i] }, new double[] { z1[i], z2[i] })
					.color(k % 2 == 0 ? 0x2CA02C : 0xFF7F0E).label(k == 0 ? "Base pair A-T" : k == 1 ? "Base pair G-C" : null));
			px[2 * k] = x1[i];
			py[2 * k] = y1[i];
			pz[2 * k] = z1[i];
			px[2 * k + 1] = x2[i];
			py[2 * k + 1] = y2[i];
			pz[2 * k + 1] = z2[i];
		}
		plot.add(Plot3D.scatter(px, py, pz).color(0x555555).size(0.12));
		plot.exportAction(true).show();
	}
}
