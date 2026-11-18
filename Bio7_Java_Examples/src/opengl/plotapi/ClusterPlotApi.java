package opengl.plotapi;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;

/**
 * The result of a cluster analysis in 3D: five clusters of points (each cluster
 * a layer with a label) and the centers of the clusters as large spheres. The
 * export action of the 3D view is enabled (HTML or Blender).
 */
public class ClusterPlotApi {

	public static void main(String[] args) {
		Random random = new Random(21);
		int clusters = 5, n = 80;
		Plot3D plot = new Plot3D().title("Cluster Analysis\nk-means with k = 5").axisTitles("PC 1", "PC 2", "PC 3");
		double[] cx = new double[clusters], cy = new double[clusters], cz = new double[clusters];
		for (int k = 0; k < clusters; k++) {
			cx[k] = random.nextGaussian() * 3;
			cy[k] = random.nextGaussian() * 3;
			cz[k] = random.nextGaussian() * 2;
			double[] x = new double[n], y = new double[n], z = new double[n];
			for (int i = 0; i < n; i++) {
				x[i] = cx[k] + random.nextGaussian() * 0.7;
				y[i] = cy[k] + random.nextGaussian() * 0.7;
				z[i] = cz[k] + random.nextGaussian() * 0.5;
			}
			plot.add(Plot3D.scatter(x, y, z).label("Cluster " + (k + 1)).size(0.1));
		}
		plot.add(Plot3D.scatter(cx, cy, cz).color(0x222222).size(0.3).label("Centers"));
		plot.exportAction(true).show();
	}
}
