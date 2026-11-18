package opengl.plot3d;

import static com.jogamp.opengl.GL2.*;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * The bacterial growth of GrowthSurfacePlot3D with a logarithmic z axis: the
 * cell counts from 1e6 to 1.3e10 are drawn as log10 values. On the log axis the
 * exponential phase of the growth is a straight line (the slope is the growth
 * rate) and the start and the capacity are visible, too (on a linear axis the
 * small populations are near 0).
 * <p>
 * The log10 values (6 - 10.11) are drawn in the coordinates of a box (z 0 - 7).
 * SpatialUtil.setAxisRanges maps the box to the log10 values (for a log axis
 * the ranges are log10 values) and SpatialUtil.setLogAxes displays the numbers
 * 1e6, 1e7, ... with minor ticks. The measurements (points with a log-normal
 * error) are drawn above (red) and below (blue) the surface. A logarithmic
 * color bar shows the colors of the surface and a legend the colors of the
 * measurements.
 */
public class GrowthLogPlot3D extends com.eco.bio7.compile.Model {

	private static final double BOX = 10, BOX_Z = 7;
	/* The range of the log10 values of the z axis */
	private static final double LOG_MIN = 6, LOG_MAX = Math.log10(GrowthSurfacePlot3D.K);
	private static final int N = 60;
	private static final int ABOVE = 0xD7301F, BELOW = 0x2B65C9;

	private final double[][] logCells = new double[N][N];
	private double[][] samples;
	private int sphereList = -1;

	public GrowthLogPlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				logCells[i][j] = Math.log10(GrowthSurfacePlot3D.population(GrowthSurfacePlot3D.time(i), GrowthSurfacePlot3D.temperature(j)));
			}
		}
		/* Measurements every 4 hours at 8 temperatures with a log-normal error */
		java.util.Random random = new java.util.Random(11);
		java.util.List<double[]> list = new java.util.ArrayList<>();
		for (double t = 0; t <= 48; t += 4) {
			for (double temp = 15; temp <= 45; temp += 4) {
				double n = GrowthSurfacePlot3D.population(t, temp) * Math.exp(0.3 * random.nextGaussian());
				double model = GrowthSurfacePlot3D.population(t, temp);
				list.add(new double[] { t, temp, Math.log10(n), Math.log10(n) - Math.log10(model) });
			}
		}
		samples = list.toArray(new double[0][]);
		SpatialUtil.setDataBounds(0, BOX, 0, BOX, 0, BOX_Z);
		SpatialUtil.setAxisRanges(GrowthSurfacePlot3D.TIME_MIN, GrowthSurfacePlot3D.TIME_MAX, GrowthSurfacePlot3D.TEMP_MIN,
				GrowthSurfacePlot3D.TEMP_MAX, LOG_MIN, LOG_MAX);
		/* Only the z axis is logarithmic */
		SpatialUtil.setLogAxes(false, false, true);
		/* The numbers of the log axis as powers of ten (10^6, 10^7, ...) */
		SpatialUtil.setAxisNumberFormats(null, null, "10^");
		SpatialUtil.setPlotView(true);
		SpatialUtil.setPlotGrids(false, true, true);
		SpatialUtil.setPlotTitle("Bacterial Growth (log scale)\nThe exponential phase is linear");
		SpatialUtil.setAxisTitles("Time [h]", "Temperature [\u00B0C]", "Cells / ml");
		/* A logarithmic color bar (the values are log10 values) and a legend of the measurements */
		SpatialUtil.setColorBar("Cells / ml", LOG_MIN, LOG_MAX, PlotLegend.VIRIDIS, true, "10^");
		SpatialUtil.setLegend("Measurements", new String[] { "Above the model", "Below the model" }, new int[] { ABOVE, BELOW });
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	/* The z coordinate of the box of a log10 value */
	private static double boxZ(double log) {
		return (log - LOG_MIN) / (LOG_MAX - LOG_MIN) * BOX_Z;
	}

	private static double boxX(double time) {
		return (time - GrowthSurfacePlot3D.TIME_MIN) / (GrowthSurfacePlot3D.TIME_MAX - GrowthSurfacePlot3D.TIME_MIN) * BOX;
	}

	private static double boxY(double temperature) {
		return (temperature - GrowthSurfacePlot3D.TEMP_MIN) / (GrowthSurfacePlot3D.TEMP_MAX - GrowthSurfacePlot3D.TEMP_MIN) * BOX;
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (sphereList == -1 || !gl.glIsList(sphereList)) {
			sphereList = gl.glGenLists(1);
			gl.glNewList(sphereList, GL_COMPILE);
			glut.glutSolidSphere(0.09, 12, 10);
			gl.glEndList();
		}
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT | GL_LINE_BIT | GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL_NORMALIZE);
		/* The measurements: above the model red, below blue */
		for (double[] s : samples) {
			PlotLegend.setColor(gl, s[3] >= 0 ? ABOVE : BELOW);
			gl.glPushMatrix();
			gl.glTranslated(boxX(s[0]), boxY(s[1]), boxZ(s[2]));
			gl.glCallList(sphereList);
			gl.glPopMatrix();
		}
		/* The model surface (transparent, after the opaque points) and its lines */
		gl.glDisable(GL_LIGHTING);
		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glDepthMask(false);
		double d = BOX / (N - 1);
		for (int j = 0; j < N - 1; j++) {
			gl.glBegin(GL_TRIANGLE_STRIP);
			for (int i = 0; i < N; i++) {
				for (int k = 0; k < 2; k++) {
					int rgb = PlotLegend.rampColor(PlotLegend.VIRIDIS, LOG_MIN, LOG_MAX, logCells[i][j + k]);
					gl.glColor4f(((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f, 0.55f);
					gl.glVertex3d(i * d, (j + k) * d, boxZ(logCells[i][j + k]));
				}
			}
			gl.glEnd();
		}
		gl.glDepthMask(true);
		gl.glColor4f(0.1f, 0.1f, 0.1f, 0.8f);
		for (int i = 0; i < N; i += 5) {
			gl.glBegin(GL_LINE_STRIP);
			for (int j = 0; j < N; j++) {
				gl.glVertex3d(i * d, j * d, boxZ(logCells[i][j]));
			}
			gl.glEnd();
			gl.glBegin(GL_LINE_STRIP);
			for (int j = 0; j < N; j++) {
				gl.glVertex3d(j * d, i * d, boxZ(logCells[j][i]));
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
