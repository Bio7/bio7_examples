package opengl.plotapi.dynamic;

import java.util.Random;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * Dynamic histograms: the evolution of a trait (e.g., the body size) in two
 * populations with different optima (selection and mutation in each
 * generation). The histograms are updated in each step of the model.
 */
public class DynamicHistogramModel extends com.eco.bio7.compile.Model {

	private static final int N = 800;
	private final Random random = new Random(3);
	private final double[][] trait = new double[2][N];
	private final double[] optimum = { 35, 65 };
	private int generation;
	private Plot3D plot;

	public DynamicHistogramModel() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i < N; i++) {
				trait[s][i] = 50 + 5 * random.nextGaussian();
			}
		}
		generation = 0;
		plot = new Plot3D().axisTitles("Body size", "Population", "Individuals").xRange(0, 100).zRange(0, 300);
		layers().show();
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private Plot3D layers() {
		return plot.title("Evolution of a Trait\nGeneration " + generation).clearLayers()
				.add(Plot3D.histogram(trait, new String[] { "Island (optimum 35)", "Mainland (optimum 65)" }).binWidth(2.5));
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (!SpatialUtil.canStep()) {
			return;
		}
		generation++;
		for (int s = 0; s < 2; s++) {
			double[] next = new double[N];
			for (int i = 0; i < N; i++) {
				/* Selection: two random parents, the one nearer to the optimum reproduces (with a mutation) */
				double a = trait[s][random.nextInt(N)], b = trait[s][random.nextInt(N)];
				double parent = Math.abs(a - optimum[s]) < Math.abs(b - optimum[s]) ? a : b;
				next[i] = Math.max(0, Math.min(100, parent + random.nextGaussian() * 1.5));
			}
			trait[s] = next;
		}
		layers().update();
	}

	public void close() {
		Plot3D.clear();
	}
}
