package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * The mean monthly temperatures of six decades as filled lines (ribbons): the
 * warming is visible as a shift of the ribbons. The colors show the
 * temperature (a color map with a color bar). The export action of the 3D view
 * is enabled (HTML or Blender).
 */
public class TemperatureRibbonsPlotApi {

	public static void main(String[] args) {
		int months = 12 * 4 + 1;
		double[] month = new double[months];
		for (int i = 0; i < months; i++) {
			month[i] = 1 + 11.0 * i / (months - 1);
		}
		Plot3D plot = new Plot3D().title("Monthly Temperature\nMeans of six decades").axisTitles("Month", "Decade", "Temperature [°C]");
		for (int d = 0; d < 6; d++) {
			double decade = 1970 + 10 * d;
			double[] y = new double[months], t = new double[months];
			for (int i = 0; i < months; i++) {
				y[i] = decade;
				/* The seasonal cycle and a warming of 0.3 degrees per decade */
				t[i] = 9 + 9 * Math.sin((month[i] - 4.2) / 12 * 2 * Math.PI) + 0.3 * d;
			}
			plot.add(Plot3D.filledLine(month, y, t).colorMap(PlotLegend.RAINBOW).alpha(0.85));
		}
		plot.zRange(-2, 20).colorBarTitle("Temperature [°C]").exportAction(true).show();
	}
}
