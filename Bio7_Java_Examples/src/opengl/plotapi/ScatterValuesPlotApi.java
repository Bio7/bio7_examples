package opengl.plotapi;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A 3D scatter plot colored by a fourth variable (with a color bar) and with
 * drop lines to the floor: sampling sites with the temperature of the soil.
 */
public class ScatterValuesPlotApi {

	public static void main(String[] args) {
		Random random = new Random(3);
		int n = 120;
		double[] east = new double[n], north = new double[n], elevation = new double[n], temperature = new double[n];
		for (int i = 0; i < n; i++) {
			east[i] = random.nextDouble() * 1000;
			north[i] = random.nextDouble() * 1000;
			double dx = (east[i] - 650) / 250, dy = (north[i] - 600) / 250;
			elevation[i] = 150 + 300 * Math.exp(-(dx * dx + dy * dy) / 2) + 10 * random.nextGaussian();
			/* Colder at higher elevations */
			temperature[i] = 14 - 0.0065 * elevation[i] * 1.5 + 0.8 * random.nextGaussian();
		}
		new Plot3D().title("Soil Temperature\nSampling sites with the elevation")
				.axisTitles("Easting [m]", "Northing [m]", "Elevation [m]")
				.add(Plot3D.scatter(east, north, elevation).values(temperature).colorMap(PlotLegend.RAINBOW).dropLines(true).size(0.14))
				.colorBarTitle("Temperature [°C]")
				.show();
	}
}
