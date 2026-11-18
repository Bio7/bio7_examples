package opengl.plotapi.dynamic;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A dynamic pie chart: the shares of five competing species (Lotka-Volterra
 * competition, the slices change in each step of the model until the
 * competition reaches an equilibrium).
 */
public class DynamicPieModel extends com.eco.bio7.compile.Model {

	private final String[] species = { "Oak", "Beech", "Pine", "Birch", "Spruce" };
	private final double[] growth = { 0.30, 0.45, 0.55, 0.80, 0.50 };
	/* The competition coefficients (the effect of species j on species i) */
	private final double[][] alpha = { { 1, 0.9, 0.5, 0.4, 0.6 }, { 0.7, 1, 0.6, 0.5, 0.8 }, { 1.1, 1.2, 1, 0.7, 0.9 }, { 1.4, 1.5, 1.2, 1, 1.3 },
			{ 0.8, 0.9, 0.7, 0.6, 1 } };
	private double[] n;
	private int year;
	private Plot3D plot;

	public DynamicPieModel() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		n = new double[] { 5, 5, 20, 40, 10 };
		year = 0;
		plot = new Plot3D();
		layers().show();
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private Plot3D layers() {
		return plot.title("Forest Succession\nYear " + year).clearLayers().add(Plot3D.pie(n.clone(), species).explode(0.1));
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (!SpatialUtil.canStep()) {
			return;
		}
		year++;
		double[] next = n.clone();
		for (int i = 0; i < n.length; i++) {
			double competition = 0;
			for (int j = 0; j < n.length; j++) {
				competition += alpha[i][j] * n[j];
			}
			next[i] = Math.max(0.01, n[i] + 0.1 * growth[i] * n[i] * (1 - competition / 100));
		}
		n = next;
		layers().update();
	}

	public void close() {
		Plot3D.clear();
	}
}
