package opengl.plotapi;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A 3D bar plot: a two dimensional histogram of correlated measurements (the
 * counts of the cells of a grid).
 */
public class BarPlotApi {

	public static void main(String[] args) {
		int bins = 12;
		double min = 0, max = 120, width = (max - min) / bins;
		double[] centers = new double[bins];
		for (int i = 0; i < bins; i++) {
			centers[i] = min + (i + 0.5) * width;
		}
		double[][] counts = new double[bins][bins];
		Random random = new Random(7);
		for (int k = 0; k < 3000; k++) {
			double a = random.nextGaussian(), b = random.nextGaussian();
			double length = 60 + 18 * a, leafWidth = 60 + 18 * (0.6 * a + 0.8 * b);
			int i = (int) Math.floor((length - min) / width), j = (int) Math.floor((leafWidth - min) / width);
			if (i >= 0 && i < bins && j >= 0 && j < bins) {
				counts[i][j]++;
			}
		}
		new Plot3D().title("Leaf Measurements\n2D histogram (n = 3000, r = 0.6)").axisTitles("Length [mm]", "Width [mm]", "Count")
				.add(Plot3D.bars(centers, centers, counts).colorMap(PlotLegend.VIRIDIS).width(0.85))
				.colorBarTitle("Count")
				.show();
	}
}
