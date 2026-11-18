package opengl.plot3d;

import static com.jogamp.opengl.GL2.*;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A surface plot z = f(x, y) in the plot view of the 3D view: a damped wave
 * (cos(r) / (1 + r / 4)) colored with a color ramp of the heights and a
 * wireframe. The wave moves with the step rate of the model (the "Time" tab of
 * the 3D options).
 * <p>
 * The function is evaluated on a regular grid. The normals of the surface are
 * computed from the neighbors for the lighting.
 */
public class SurfacePlot3D extends com.eco.bio7.compile.Model {

	private static final int N = 80;
	private static final double MIN = -10, MAX = 10;
	private static final double Z_MIN = -5, Z_MAX = 5;

	private final double[][] z = new double[N][N];
	private double phase = 0;

	public SurfacePlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		compute();
		SpatialUtil.setDataBounds(MIN, MAX, MIN, MAX, Z_MIN, Z_MAX);
		SpatialUtil.setPlotView(true);
		/* The surface covers the floor: only the walls */
		SpatialUtil.setPlotGrids(false, true, true);
		SpatialUtil.setPlotTitle("Damped Wave\nz = 5 cos(r - t) / (1 + r / 4)");
		SpatialUtil.setAxisTitles("x", "y", "z");
		/* The color bar of the heights (the same ramp as the colors of the surface) */
		SpatialUtil.setColorBar("Height z", Z_MIN, Z_MAX, PlotLegend.RAINBOW);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	/* The function of the surface: a damped wave around the center */
	private double f(double x, double y) {
		double r = Math.sqrt(x * x + y * y);
		return 5 * Math.cos(r - phase) / (1 + 0.25 * r);
	}

	private void compute() {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				z[i][j] = f(x(i), y(j));
			}
		}
	}

	private static double x(int i) {
		return MIN + (MAX - MIN) * i / (N - 1);
	}

	private static double y(int j) {
		return MIN + (MAX - MIN) * j / (N - 1);
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (SpatialUtil.canStep()) {
			phase += 0.1;
			compute();
		}
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT | GL_POLYGON_BIT | GL_LINE_BIT);
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		gl.glLightModeli(GL_LIGHT_MODEL_TWO_SIDE, GL_TRUE);
		gl.glEnable(GL_NORMALIZE);
		/* The wireframe is drawn on top of the filled surface */
		gl.glEnable(GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(1, 1);
		double dx = (MAX - MIN) / (N - 1);
		for (int j = 0; j < N - 1; j++) {
			gl.glBegin(GL_TRIANGLE_STRIP);
			for (int i = 0; i < N; i++) {
				for (int k = 0; k < 2; k++) {
					int jj = j + k;
					color(gl, z[i][jj]);
					/* The normal from the differences of the neighbors */
					double zx = (z[Math.min(N - 1, i + 1)][jj] - z[Math.max(0, i - 1)][jj]) / (2 * dx);
					double zy = (z[i][Math.min(N - 1, jj + 1)] - z[i][Math.max(0, jj - 1)]) / (2 * dx);
					gl.glNormal3d(-zx, -zy, 1);
					gl.glVertex3d(x(i), y(jj), z[i][jj]);
				}
			}
			gl.glEnd();
		}
		gl.glDisable(GL_LIGHTING);
		gl.glDisable(GL_POLYGON_OFFSET_FILL);
		gl.glColor4f(0.1f, 0.1f, 0.1f, 1);
		gl.glLineWidth(1);
		for (int i = 0; i < N; i += 4) {
			gl.glBegin(GL_LINE_STRIP);
			for (int j = 0; j < N; j++) {
				gl.glVertex3d(x(i), y(j), z[i][j]);
			}
			gl.glEnd();
			gl.glBegin(GL_LINE_STRIP);
			for (int j = 0; j < N; j++) {
				gl.glVertex3d(x(j), y(i), z[j][i]);
			}
			gl.glEnd();
		}
		gl.glPopAttrib();
	}

	/* The color of the height (the ramp of the color bar) */
	private static void color(GL2 gl, double value) {
		PlotLegend.setColor(gl, PlotLegend.rampColor(PlotLegend.RAINBOW, Z_MIN, Z_MAX, value));
	}

	public void close() {
		SpatialUtil.clearDataBounds();
		SpatialUtil.resetPlotLabels();
	}
}
