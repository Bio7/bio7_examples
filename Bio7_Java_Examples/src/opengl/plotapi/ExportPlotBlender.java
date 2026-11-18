package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * Exports a plot without the 3D view: an HTML page with WebGL and the files for
 * Blender (a glTF file and a Python script, in Blender: Scripting - Open - Run
 * Script). The files are written to the home folder. The plot is displayed in
 * the 3D view, too (with the export action of the toolbar).
 */
public class ExportPlotBlender {

	public static void main(String[] args) throws Exception {
		int n = 60;
		double[] x = new double[n], y = new double[n];
		double[][] z = new double[n][n];
		for (int i = 0; i < n; i++) {
			x[i] = -2 + 4.0 * i / (n - 1);
			y[i] = -2 + 4.0 * i / (n - 1);
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				/* A saddle with a ripple */
				z[i][j] = x[i] * x[i] - y[j] * y[j] + 0.3 * Math.sin(3 * x[i]) * Math.cos(3 * y[j]);
			}
		}
		Plot3D plot = new Plot3D().title("Saddle with a Ripple\nz = x^2 − y^2 + 0.3 sin(3x) cos(3y)").axisTitles("x", "y", "z")
				.add(Plot3D.surface(x, y, z).colorMap(PlotLegend.DIVERGING))
				.add(Plot3D.contour(x, y, z).levels(14).onFloor(true).colorMap(PlotLegend.DIVERGING));
		String home = System.getProperty("user.home");
		plot.exportHtml(home + "/Saddle.html");
		String script = plot.exportBlender(home + "/Saddle.glb");
		System.out.println("HTML page: " + home + "/Saddle.html");
		System.out.println("Blender: " + home + "/Saddle.glb and " + script);
		plot.exportAction(true).show();
	}
}
