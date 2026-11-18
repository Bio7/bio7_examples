package opengl.plotapi;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * Logarithmic axes: the metabolic rate of animals over the body mass and the
 * temperature (an allometric power law with B = b_0 M^{3/4} e^{-E/kT}). The data
 * is given in the original values, the x and z axes are logarithmic and the
 * numbers are displayed as powers of ten.
 */
public class LogAxesPlotApi {

	public static void main(String[] args) {
		Random random = new Random(5);
		int n = 300;
		double[] mass = new double[n], temperature = new double[n], rate = new double[n];
		for (int i = 0; i < n; i++) {
			/* 1 mg - 1000 kg (in g) */
			mass[i] = Math.pow(10, -3 + random.nextDouble() * 9);
			temperature[i] = 5 + random.nextDouble() * 35;
			double kT = 8.617e-5 * (temperature[i] + 273.15);
			rate[i] = 4e9 * Math.pow(mass[i], 0.75) * Math.exp(-0.65 / kT) * Math.exp(0.3 * random.nextGaussian());
		}
		new Plot3D().title("Metabolic Rate\nB = b_0 · M^{3/4} · e^{−E/kT}")
				.axisTitles("Body mass [g]", "Temperature [°C]", "Metabolic rate [W]")
				.logAxes(true, false, true)
				.numberFormats("10^", null, "10^")
				.add(Plot3D.scatter(mass, temperature, rate).values(temperature).colorMap(PlotLegend.RAINBOW).label("Animals"))
				.colorBarTitle("Temperature [°C]")
				.show();
	}
}
