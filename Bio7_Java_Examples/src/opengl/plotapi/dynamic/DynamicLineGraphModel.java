package opengl.plotapi.dynamic;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A dynamic line graph: the populations of a food chain (resource, consumer,
 * predator; Lotka-Volterra with a logistic resource) over the last 200 steps
 * (a moving window of the time axis). The line graph is updated in each step
 * of the model.
 */
public class DynamicLineGraphModel extends com.eco.bio7.compile.Model {

	private static final int WINDOW = 200;
	private final double[][] history = new double[3][WINDOW];
	private final double[] time = new double[WINDOW];
	private double r, c, p;
	private int t;
	private Plot3D plot;

	public DynamicLineGraphModel() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		r = 60;
		c = 20;
		p = 5;
		t = 0;
		for (int i = 0; i < WINDOW; i++) {
			time[i] = i - WINDOW;
			history[0][i] = r;
			history[1][i] = c;
			history[2][i] = p;
		}
		plot = new Plot3D().axisTitles("Time", "Population", "Individuals").zRange(0, 120).aspect(1.6, 0.8, 0.8);
		layers().show();
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private Plot3D layers() {
		return plot.title("Food Chain\nStep " + t).clearLayers()
				.add(Plot3D.lineGraph(time, history).names("Resource", "Consumer", "Predator").markers(false, 0).fill(true));
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (!SpatialUtil.canStep()) {
			return;
		}
		/* Some small integration steps */
		for (int k = 0; k < 10; k++) {
			double dr = 0.5 * r * (1 - r / 100) - 0.02 * r * c;
			double dc = 0.01 * r * c - 0.2 * c - 0.03 * c * p;
			double dp = 0.008 * c * p - 0.1 * p;
			r = Math.max(0.1, r + 0.05 * dr);
			c = Math.max(0.1, c + 0.05 * dc);
			p = Math.max(0.1, p + 0.05 * dp);
		}
		t++;
		/* The moving window: the oldest value is removed */
		for (int s = 0; s < 3; s++) {
			System.arraycopy(history[s], 1, history[s], 0, WINDOW - 1);
		}
		System.arraycopy(time, 1, time, 0, WINDOW - 1);
		history[0][WINDOW - 1] = r;
		history[1][WINDOW - 1] = c;
		history[2][WINDOW - 1] = p;
		time[WINDOW - 1] = t;
		plot.xRange(t - WINDOW, t);
		layers().update();
	}

	public void close() {
		Plot3D.clear();
	}
}
