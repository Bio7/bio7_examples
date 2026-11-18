package opengl.plotapi.dynamic;

import java.util.Random;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A dynamic scatter plot: random walkers of two species which drift to their
 * centers (the plot is updated in each step of the model with Plot3D.update).
 * The model starts automatically, the speed is the step rate in the "Time" tab
 * of the 3D options.
 */
public class DynamicScatterModel extends com.eco.bio7.compile.Model {

	private static final int N = 200;
	private final Random random = new Random(1);
	private final double[][] x = new double[2][N], y = new double[2][N], z = new double[2][N];
	private final double[][] centers = { { 30, 30, 65 }, { 70, 70, 35 } };
	private Plot3D plot;
	private int t;

	public DynamicScatterModel() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i < N; i++) {
				x[s][i] = random.nextDouble() * 100;
				y[s][i] = random.nextDouble() * 100;
				z[s][i] = random.nextDouble() * 100;
			}
		}
		t = 0;
		/* Fixed ranges: the axes don't change with the data */
		plot = new Plot3D().axisTitles("x [m]", "y [m]", "z [m]").xRange(0, 100).yRange(0, 100).zRange(0, 100);
		layers().show();
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private Plot3D layers() {
		return plot.title("Random Walkers\nStep " + t).clearLayers().add(Plot3D.scatter(x[0], y[0], z[0]).label("Species A").size(0.12))
				.add(Plot3D.scatter(x[1], y[1], z[1]).label("Species B").size(0.12));
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (!SpatialUtil.canStep()) {
			return;
		}
		t++;
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i < N; i++) {
				x[s][i] = clamp(x[s][i] + random.nextGaussian() * 2 + (centers[s][0] - x[s][i]) * 0.02);
				y[s][i] = clamp(y[s][i] + random.nextGaussian() * 2 + (centers[s][1] - y[s][i]) * 0.02);
				z[s][i] = clamp(z[s][i] + random.nextGaussian() * 2 + (centers[s][2] - z[s][i]) * 0.02);
			}
		}
		layers().update();
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(100, v));
	}

	public void close() {
		Plot3D.clear();
	}
}
