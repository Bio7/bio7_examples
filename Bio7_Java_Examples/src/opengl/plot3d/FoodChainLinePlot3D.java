package opengl.plot3d;

import static com.jogamp.opengl.GL2.*;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A 3D line plot of a trajectory: the chaotic dynamics of the food chain model
 * of Hastings and Powell (1991, Chaos in a three-species food chain) with a
 * resource (x), a consumer (y) and a predator (z). The trajectory is colored
 * with the time and a sphere moves along the trajectory with the step rate of
 * the model (the "Time" tab of the 3D options).
 * <p>
 * The three populations have different ranges (x: 0 - 1, y: 0 - 0.5, z: 7 -
 * 11). The 3D view displays all axes with the same scale. Therefore the data is
 * drawn in the coordinates of a box (0 - 10) and SpatialUtil.setAxisRanges
 * displays the values of the populations at the axes.
 */
public class FoodChainLinePlot3D extends com.eco.bio7.compile.Model {

	/* The parameters of Hastings and Powell (1991) */
	private static final double A1 = 5, B1 = 3, A2 = 0.1, B2 = 2, D1 = 0.4, D2 = 0.01;
	private static final double DT = 0.05;
	private static final int STEPS = 60000;
	private static final double BOX = 10;
	/* The colors of the time */
	private static final int[] TIME_COLORS = { 0x2633BF, 0x8C3399, 0xE6731A };

	/* The trajectory (x, y, z) and the ranges of the populations */
	private double[][] trajectory;
	private final double[] min = new double[3], max = new double[3];
	private int current = 0;
	private int sphereList = -1;

	public FoodChainLinePlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		integrate();
		SpatialUtil.setDataBounds(0, BOX, 0, BOX, 0, BOX);
		/* The numbers of the axes: the values of the populations at the box */
		SpatialUtil.setAxisRanges(min[0], max[0], min[1], max[1], min[2], max[2]);
		SpatialUtil.setPlotView(true);
		SpatialUtil.setPlotGrids(true, true, true);
		SpatialUtil.setPlotTitle("Hastings-Powell Food Chain\nChaotic dynamics of three species");
		SpatialUtil.setAxisTitles("Resource", "Consumer", "Predator");
		SpatialUtil.setColorBar("Time", 0, STEPS * DT, TIME_COLORS);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private static double[] derivative(double[] p) {
		double x = p[0], y = p[1], z = p[2];
		double f1 = A1 * x / (1 + B1 * x);
		double f2 = A2 * y / (1 + B2 * y);
		return new double[] { x * (1 - x) - f1 * y, f1 * y - f2 * z - D1 * y, f2 * z - D2 * z };
	}

	/* Runge-Kutta (4th order) integration without the transient */
	private void integrate() {
		double[] p = { 0.75, 0.15, 10 };
		trajectory = new double[STEPS][];
		for (int i = -10000; i < STEPS; i++) {
			double[] k1 = derivative(p);
			double[] k2 = derivative(add(p, k1, DT / 2));
			double[] k3 = derivative(add(p, k2, DT / 2));
			double[] k4 = derivative(add(p, k3, DT));
			for (int k = 0; k < 3; k++) {
				p[k] += DT / 6 * (k1[k] + 2 * k2[k] + 2 * k3[k] + k4[k]);
			}
			if (i >= 0) {
				trajectory[i] = p.clone();
			}
		}
		for (int k = 0; k < 3; k++) {
			min[k] = Double.MAX_VALUE;
			max[k] = -Double.MAX_VALUE;
			for (double[] q : trajectory) {
				min[k] = Math.min(min[k], q[k]);
				max[k] = Math.max(max[k], q[k]);
			}
		}
	}

	private static double[] add(double[] p, double[] d, double h) {
		return new double[] { p[0] + h * d[0], p[1] + h * d[1], p[2] + h * d[2] };
	}

	/* The coordinate of the box of a value of the population k */
	private double box(int k, double value) {
		return (value - min[k]) / (max[k] - min[k]) * BOX;
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (sphereList == -1 || !gl.glIsList(sphereList)) {
			sphereList = gl.glGenLists(1);
			gl.glNewList(sphereList, GL_COMPILE);
			glut.glutSolidSphere(0.18, 16, 12);
			gl.glEndList();
		}
		if (SpatialUtil.canStep()) {
			current = (current + 40) % trajectory.length;
		}
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT | GL_LINE_BIT);
		gl.glDisable(GL_LIGHTING);
		gl.glLineWidth(1.5f);
		gl.glBegin(GL_LINE_STRIP);
		for (int i = 0; i < trajectory.length; i++) {
			/* The color of the time (the ramp of the color bar) */
			PlotLegend.setColor(gl, PlotLegend.rampColor(TIME_COLORS, 0, trajectory.length - 1, i));
			double[] q = trajectory[i];
			gl.glVertex3d(box(0, q[0]), box(1, q[1]), box(2, q[2]));
		}
		gl.glEnd();
		/* The current state */
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL_NORMALIZE);
		gl.glColor3f(0.85f, 0.1f, 0.1f);
		double[] q = trajectory[current];
		gl.glPushMatrix();
		gl.glTranslated(box(0, q[0]), box(1, q[1]), box(2, q[2]));
		gl.glCallList(sphereList);
		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	public void close() {
		SpatialUtil.clearDataBounds();
		SpatialUtil.resetPlotLabels();
	}
}
