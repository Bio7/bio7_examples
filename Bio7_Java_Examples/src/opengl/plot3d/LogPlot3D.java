package opengl.plot3d;

import static com.jogamp.opengl.GL2.*;

import java.util.Random;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A 3D plot with logarithmic axes in the plot view of the 3D view: values over
 * several orders of magnitude, e.g., a power law z = c * x^a * y^b (like the
 * allometric relations in ecology) with log-normal errors.
 * <p>
 * The data is drawn as log10 values and SpatialUtil.setLogAxes displays the
 * axes logarithmic: the numbers are the values (0.01, 1, 100, 1e4) with minor
 * ticks and grid lines at 2, 3, ..., 9 times the powers of ten. In log-log-log
 * coordinates a power law is a plane: log10(z) = log10(c) + a * log10(x) + b *
 * log10(y). The exponents a, b are estimated with a linear regression of the
 * log values (displayed in the title and printed in the console) and the fitted
 * plane is drawn with the data. Values <= 0 have no logarithm and are not
 * plotted.
 */
public class LogPlot3D extends com.eco.bio7.compile.Model {

	/* The parameters of the power law */
	private static final double C = 0.5, A = 0.75, B = -0.5;
	/* The range of x and y: 1 - 10000 (the exponents 0 - 4) */
	private static final double LOG_MIN = 0, LOG_MAX = 4;
	/* The range of the log10 z values */
	private static final double Z_MIN = -3, Z_MAX = 3;
	/* The range of the residuals of the color bar */
	private static final double RESIDUAL = 0.3;

	private double[][] logPoints;
	/* The fitted plane: log10(z) = coefficients[0] + coefficients[1] * log10(x) + coefficients[2] * log10(y) */
	private double[] coefficients;
	private int sphereList = -1;

	public LogPlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		createData();
		coefficients = fitPlane(logPoints);
		System.out.printf("Fitted power law: z = %.3f * x^%.3f * y^%.3f (true: z = %.3f * x^%.3f * y^%.3f)%n",
				Math.pow(10, coefficients[0]), coefficients[1], coefficients[2], C, A, B);
		SpatialUtil.setDataBounds(LOG_MIN, LOG_MAX, LOG_MIN, LOG_MAX, Z_MIN, Z_MAX);
		SpatialUtil.setPlotView(true);
		/* Logarithmic axes: the data are log10 values, the numbers 10^v */
		SpatialUtil.setLogAxes(true, true, true);
		/* The numbers of the log axes as powers of ten with superscripts (10^0, 10^1, ...) */
		SpatialUtil.setAxisNumberFormats("10^", "10^", "10^");
		SpatialUtil.setPlotGrids(true, true, true);
		SpatialUtil.setPlotTitle(String.format(java.util.Locale.ROOT, "Power Law z = %.1f \u00B7 x^{%.2f} \u00B7 y^{%.2f}\nFitted: z = %.2f \u00B7 x^{%.3f} \u00B7 y^{%.3f}", C, A, B,
				Math.pow(10, coefficients[0]), coefficients[1], coefficients[2]));
		SpatialUtil.setAxisTitles("x", "y", "z");
		/* The colors of the residuals (log10(z) - log10(fitted z)) */
		SpatialUtil.setColorBar("Residual (log_{10})", -RESIDUAL, RESIDUAL, PlotLegend.DIVERGING);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	/* The data in the original units and its log10 values */
	private void createData() {
		Random random = new Random(3);
		int n = 400;
		double[][] log = new double[n][];
		int count = 0;
		for (int i = 0; i < n; i++) {
			/* x and y uniform on the log scale (1 - 10000) */
			double x = Math.pow(10, LOG_MIN + random.nextDouble() * (LOG_MAX - LOG_MIN));
			double y = Math.pow(10, LOG_MIN + random.nextDouble() * (LOG_MAX - LOG_MIN));
			/* A log-normal error (multiplicative) */
			double z = C * Math.pow(x, A) * Math.pow(y, B) * Math.exp(0.35 * random.nextGaussian());
			if (x <= 0 || y <= 0 || z <= 0) {
				continue;
			}
			double lz = Math.log10(z);
			/* Values outside the plot are not displayed */
			if (lz < Z_MIN || lz > Z_MAX) {
				continue;
			}
			log[count++] = new double[] { Math.log10(x), Math.log10(y), lz };
		}
		logPoints = java.util.Arrays.copyOf(log, count);
	}

	/* The least squares plane z = b0 + b1 * x + b2 * y (the normal equations) */
	private static double[] fitPlane(double[][] p) {
		double[][] m = new double[3][4];
		for (double[] v : p) {
			double[] row = { 1, v[0], v[1] };
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					m[i][j] += row[i] * row[j];
				}
				m[i][3] += row[i] * v[2];
			}
		}
		/* Gaussian elimination */
		for (int c = 0; c < 3; c++) {
			int pivot = c;
			for (int r = c + 1; r < 3; r++) {
				if (Math.abs(m[r][c]) > Math.abs(m[pivot][c])) {
					pivot = r;
				}
			}
			double[] t = m[c];
			m[c] = m[pivot];
			m[pivot] = t;
			for (int r = 0; r < 3; r++) {
				if (r != c) {
					double f = m[r][c] / m[c][c];
					for (int k = c; k < 4; k++) {
						m[r][k] -= f * m[c][k];
					}
				}
			}
		}
		return new double[] { m[0][3] / m[0][0], m[1][3] / m[1][1], m[2][3] / m[2][2] };
	}

	private double plane(double lx, double ly) {
		return coefficients[0] + coefficients[1] * lx + coefficients[2] * ly;
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (sphereList == -1 || !gl.glIsList(sphereList)) {
			sphereList = gl.glGenLists(1);
			gl.glNewList(sphereList, GL_COMPILE);
			glut.glutSolidSphere(0.035, 10, 8);
			gl.glEndList();
		}
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT | GL_LINE_BIT | GL_COLOR_BUFFER_BIT
				| GL_DEPTH_BUFFER_BIT);
		/* The data: the color shows the distance to the fitted plane (above red, below blue) */
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL_NORMALIZE);
		for (double[] p : logPoints) {
			double residual = p[2] - plane(p[0], p[1]);
			PlotLegend.setColor(gl, PlotLegend.rampColor(PlotLegend.DIVERGING, -RESIDUAL, RESIDUAL, residual));
			gl.glPushMatrix();
			gl.glTranslated(p[0], p[1], p[2]);
			gl.glCallList(sphereList);
			gl.glPopMatrix();
		}
		/* The fitted plane (transparent, drawn after the opaque points) */
		gl.glDisable(GL_LIGHTING);
		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glDepthMask(false);
		gl.glColor4f(0.3f, 0.6f, 0.3f, 0.35f);
		gl.glBegin(GL_QUADS);
		gl.glVertex3d(LOG_MIN, LOG_MIN, clampZ(plane(LOG_MIN, LOG_MIN)));
		gl.glVertex3d(LOG_MAX, LOG_MIN, clampZ(plane(LOG_MAX, LOG_MIN)));
		gl.glVertex3d(LOG_MAX, LOG_MAX, clampZ(plane(LOG_MAX, LOG_MAX)));
		gl.glVertex3d(LOG_MIN, LOG_MAX, clampZ(plane(LOG_MIN, LOG_MAX)));
		gl.glEnd();
		gl.glDepthMask(true);
		/* The lines of the decades on the plane */
		gl.glColor4f(0.15f, 0.4f, 0.15f, 0.9f);
		gl.glLineWidth(1.5f);
		for (double d = LOG_MIN; d <= LOG_MAX + 1e-9; d += 1) {
			gl.glBegin(GL_LINES);
			gl.glVertex3d(d, LOG_MIN, clampZ(plane(d, LOG_MIN)));
			gl.glVertex3d(d, LOG_MAX, clampZ(plane(d, LOG_MAX)));
			gl.glVertex3d(LOG_MIN, d, clampZ(plane(LOG_MIN, d)));
			gl.glVertex3d(LOG_MAX, d, clampZ(plane(LOG_MAX, d)));
			gl.glEnd();
		}
		gl.glPopAttrib();
	}

	private static double clampZ(double z) {
		return Math.max(Z_MIN, Math.min(Z_MAX, z));
	}

	public void close() {
		SpatialUtil.clearDataBounds();
		SpatialUtil.resetPlotLabels();
	}
}
