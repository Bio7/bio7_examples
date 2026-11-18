package opengl.plotapi.charts;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;

/**
 * Histograms of three groups in the depth (the bins of all groups are the
 * same): the body length of three species. The export action of the 3D view is
 * enabled (HTML or Blender).
 */
public class HistogramPlotApi {

	public static void main(String[] args) {
		Random random = new Random(4);
		String[] species = { "Species A", "Species B", "Species C" };
		double[][] length = new double[3][];
		double[] means = { 22, 30, 36 }, sds = { 3, 4, 2.5 };
		for (int k = 0; k < 3; k++) {
			length[k] = new double[400];
			for (int i = 0; i < length[k].length; i++) {
				length[k][i] = means[k] + sds[k] * random.nextGaussian();
			}
		}
		new Plot3D().title("Body Length\nHistograms of three species").axisTitles("Body length [mm]", "Species", "Count")
				.add(Plot3D.histogram(length, species).bins(24))
				.exportAction(true)
				.show();
	}
}
