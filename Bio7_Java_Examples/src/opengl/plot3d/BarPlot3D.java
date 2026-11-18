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
 * A 3D bar plot (a two dimensional histogram) in the plot view of the 3D view:
 * the counts of 2000 correlated, normally distributed measurements (e.g., the
 * length and the width of leaves in mm) in the cells of a 12 x 12 grid. The
 * heights of the bars are the counts, the colors a color ramp of the counts.
 * <p>
 * The 3D view displays the three axes with the same scale. Therefore the units
 * of the data are chosen with similar ranges (x, y: 0 - 120 mm, z: about 0 -
 * 100 counts).
 */
public class BarPlot3D extends com.eco.bio7.compile.Model {

	private static final int BINS = 12;
	private static final double MIN = 0, MAX = 120;
	/* The color ramp of the counts (blue - yellow - red) */
	private static final int[] RAMP = { 0x3359CC, 0xF2D91A, 0xD9261A };

	private final int[][] counts = new int[BINS][BINS];
	private int maxCount;

	public BarPlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		createHistogram();
		/* The counts are scaled to the plot (z: 0 - maximum count) */
		SpatialUtil.setDataBounds(MIN, MAX, MIN, MAX, 0, maxCount);
		SpatialUtil.setPlotView(true);
		SpatialUtil.setPlotGrids(false, true, true);
		SpatialUtil.setPlotTitle("Leaf Measurements\n2D histogram (n = 2000, r = 0.6)");
		SpatialUtil.setAxisTitles("Length [mm]", "Width [mm]", "Count");
		SpatialUtil.setColorBar("Count", 0, maxCount, RAMP);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private void createHistogram() {
		Random random = new Random(7);
		double rho = 0.6;
		for (int i = 0; i < 2000; i++) {
			double a = random.nextGaussian(), b = random.nextGaussian();
			/* Correlated values (correlation rho) around the center */
			double x = 60 + 20 * a;
			double y = 60 + 20 * (rho * a + Math.sqrt(1 - rho * rho) * b);
			int bx = (int) Math.floor((x - MIN) / (MAX - MIN) * BINS);
			int by = (int) Math.floor((y - MIN) / (MAX - MIN) * BINS);
			if (bx >= 0 && bx < BINS && by >= 0 && by < BINS) {
				counts[bx][by]++;
			}
		}
		maxCount = 1;
		for (int[] row : counts) {
			for (int c : row) {
				maxCount = Math.max(maxCount, c);
			}
		}
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		double cell = (MAX - MIN) / BINS;
		double gap = 0.1 * cell;
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT);
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL_NORMALIZE);
		for (int i = 0; i < BINS; i++) {
			for (int j = 0; j < BINS; j++) {
				if (counts[i][j] == 0) {
					continue;
				}
				double x0 = MIN + i * cell + gap, x1 = MIN + (i + 1) * cell - gap;
				double y0 = MIN + j * cell + gap, y1 = MIN + (j + 1) * cell - gap;
				PlotLegend.setColor(gl, PlotLegend.rampColor(RAMP, 0, maxCount, counts[i][j]));
				box(gl, x0, x1, y0, y1, 0, counts[i][j]);
			}
		}
		gl.glPopAttrib();
	}

	/* A box with the normals of the sides */
	private static void box(GL2 gl, double x0, double x1, double y0, double y1, double z0, double z1) {
		gl.glBegin(GL_QUADS);
		gl.glNormal3d(0, 0, 1);
		gl.glVertex3d(x0, y0, z1);
		gl.glVertex3d(x1, y0, z1);
		gl.glVertex3d(x1, y1, z1);
		gl.glVertex3d(x0, y1, z1);
		gl.glNormal3d(0, 0, -1);
		gl.glVertex3d(x0, y0, z0);
		gl.glVertex3d(x0, y1, z0);
		gl.glVertex3d(x1, y1, z0);
		gl.glVertex3d(x1, y0, z0);
		gl.glNormal3d(0, -1, 0);
		gl.glVertex3d(x0, y0, z0);
		gl.glVertex3d(x1, y0, z0);
		gl.glVertex3d(x1, y0, z1);
		gl.glVertex3d(x0, y0, z1);
		gl.glNormal3d(0, 1, 0);
		gl.glVertex3d(x0, y1, z0);
		gl.glVertex3d(x0, y1, z1);
		gl.glVertex3d(x1, y1, z1);
		gl.glVertex3d(x1, y1, z0);
		gl.glNormal3d(-1, 0, 0);
		gl.glVertex3d(x0, y0, z0);
		gl.glVertex3d(x0, y0, z1);
		gl.glVertex3d(x0, y1, z1);
		gl.glVertex3d(x0, y1, z0);
		gl.glNormal3d(1, 0, 0);
		gl.glVertex3d(x1, y0, z0);
		gl.glVertex3d(x1, y1, z0);
		gl.glVertex3d(x1, y1, z1);
		gl.glVertex3d(x1, y0, z1);
		gl.glEnd();
	}


	public void close() {
		SpatialUtil.clearDataBounds();
		SpatialUtil.resetPlotLabels();
	}
}
