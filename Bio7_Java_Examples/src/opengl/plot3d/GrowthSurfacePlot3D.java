package opengl.plot3d;

import static com.jogamp.opengl.GL2.*;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A surface plot with large numbers: the logistic growth of bacteria (cells per
 * ml up to 1.3e10) in dependence of the time and the temperature (the growth
 * rate has an optimum at 37 degrees).
 * <p>
 * The time (hours), the temperature (degrees) and the cell counts have very
 * different ranges. The data is drawn in the coordinates of a box and
 * SpatialUtil.setAxisRanges displays the values at the axes. Large numbers are
 * displayed in a scientific notation (e.g., 1.2e10). See GrowthLogPlot3D for
 * the same model with a logarithmic axis.
 */
public class GrowthSurfacePlot3D extends com.eco.bio7.compile.Model {

	/* The capacity, the start population, the maximum growth rate (1/h), the optimum and the width of the temperature */
	static final double K = 1.3e10, N0 = 1e6, R_MAX = 0.9, T_OPT = 37, T_WIDTH = 8;
	static final double TIME_MIN = 0, TIME_MAX = 48, TEMP_MIN = 10, TEMP_MAX = 45;
	/* The box: x, y 0 - 10, z 0 - 7 */
	private static final double BOX = 10, BOX_Z = 7;
	private static final int N = 60;

	private final double[][] cells = new double[N][N];

	public GrowthSurfacePlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				cells[i][j] = population(time(i), temperature(j));
			}
		}
		SpatialUtil.setDataBounds(0, BOX, 0, BOX, 0, BOX_Z);
		SpatialUtil.setAxisRanges(TIME_MIN, TIME_MAX, TEMP_MIN, TEMP_MAX, 0, K);
		SpatialUtil.setPlotView(true);
		SpatialUtil.setPlotGrids(false, true, true);
		SpatialUtil.setPlotTitle("Bacterial Growth\nN(t) = K / (1 + (K / N_0 \u2212 1) \u00B7 e^{\u2212r(T) t})");
		SpatialUtil.setAxisTitles("Time [h]", "Temperature [\u00B0C]", "Cells / ml");
		SpatialUtil.setColorBar("Cells / ml", 0, K, PlotLegend.VIRIDIS);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	/* The logistic growth with the rate of the temperature */
	static double population(double t, double temperature) {
		double d = (temperature - T_OPT) / T_WIDTH;
		double r = R_MAX * Math.exp(-d * d);
		return K / (1 + (K / N0 - 1) * Math.exp(-r * t));
	}

	static double time(int i) {
		return TIME_MIN + (TIME_MAX - TIME_MIN) * i / (N - 1);
	}

	static double temperature(int j) {
		return TEMP_MIN + (TEMP_MAX - TEMP_MIN) * j / (N - 1);
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT | GL_POLYGON_BIT | GL_LINE_BIT);
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		gl.glLightModeli(GL_LIGHT_MODEL_TWO_SIDE, GL_TRUE);
		gl.glEnable(GL_NORMALIZE);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(1, 1);
		double d = BOX / (N - 1);
		for (int j = 0; j < N - 1; j++) {
			gl.glBegin(GL_TRIANGLE_STRIP);
			for (int i = 0; i < N; i++) {
				for (int k = 0; k < 2; k++) {
					int jj = j + k;
					double z = cells[i][jj] / K * BOX_Z;
					PlotLegend.setColor(gl, PlotLegend.rampColor(PlotLegend.VIRIDIS, 0, K, cells[i][jj]));
					double zx = (cells[Math.min(N - 1, i + 1)][jj] - cells[Math.max(0, i - 1)][jj]) / K * BOX_Z / (2 * d);
					double zy = (cells[i][Math.min(N - 1, jj + 1)] - cells[i][Math.max(0, jj - 1)]) / K * BOX_Z / (2 * d);
					gl.glNormal3d(-zx, -zy, 1);
					gl.glVertex3d(i * d, jj * d, z);
				}
			}
			gl.glEnd();
		}
		/* The lines of every 6 hours and 5 degrees */
		gl.glDisable(GL_LIGHTING);
		gl.glColor3f(0.15f, 0.15f, 0.15f);
		for (int i = 0; i < N; i += 5) {
			gl.glBegin(GL_LINE_STRIP);
			for (int j = 0; j < N; j++) {
				gl.glVertex3d(i * d, j * d, cells[i][j] / K * BOX_Z);
			}
			gl.glEnd();
			gl.glBegin(GL_LINE_STRIP);
			for (int j = 0; j < N; j++) {
				gl.glVertex3d(j * d, i * d, cells[j][i] / K * BOX_Z);
			}
			gl.glEnd();
		}
		gl.glPopAttrib();
	}

	public void close() {
		SpatialUtil.clearDataBounds();
		SpatialUtil.resetPlotLabels();
	}
}
