package opengl.plotapi.dynamic;

import java.util.Random;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A dynamic violin plot: the growth of the trees of four cohorts (the height
 * distributions spread and shift with the years, the light competition makes
 * the distributions skewed). The violins are updated in each step of the
 * model.
 */
public class DynamicViolinModel extends com.eco.bio7.compile.Model {

	private static final int N = 300;
	private final Random random = new Random(5);
	private final double[][] height = new double[4][N];
	private final double[] rates = { 0.9, 0.7, 0.55, 0.4 };
	private int year;
	private Plot3D plot;

	public DynamicViolinModel() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		for (int s = 0; s < 4; s++) {
			for (int i = 0; i < N; i++) {
				height[s][i] = 1 + 2 * random.nextDouble();
			}
		}
		year = 0;
		plot = new Plot3D().axisTitles("Cohort", "", "Height [m]").zRange(0, 40);
		layers().show();
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private Plot3D layers() {
		return plot.title("Tree Growth\nYear " + year).clearLayers()
				.add(Plot3D.violin(height, new String[] { "Sunny", "Light shade", "Shade", "Deep shade" }).bandwidth(0.8));
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (!SpatialUtil.canStep() || year >= 60) {
			return;
		}
		year++;
		for (int s = 0; s < 4; s++) {
			for (int i = 0; i < N; i++) {
				/* Logistic growth (maximum 38 m) with a random vigor of each tree */
				double h = height[s][i], vigor = 0.6 + 0.8 * ((i * 7919) % N) / (double) N;
				height[s][i] = h + rates[s] * vigor * h * (1 - h / 38) * 0.12 + 0.05 * random.nextGaussian();
			}
		}
		layers().update();
	}

	public void close() {
		Plot3D.clear();
	}
}
