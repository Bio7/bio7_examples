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
 * A 3D scatter plot with drop lines: sampling sites with the coordinates of a
 * map (UTM easting and northing in meters) and the elevation. Each site has a
 * line to the floor and a shadow on the floor (the position on the map). The
 * colors show the elevation.
 * <p>
 * The coordinates of the map are large numbers (e.g., 345200). They are
 * displayed with the format "%.0f" (SpatialUtil.setAxisNumberFormats) instead
 * of the automatic scientific notation (3.452e5). The data is drawn in the
 * coordinates of a box and SpatialUtil.setAxisRanges displays the values.
 */
public class DropLinesPlot3D extends com.eco.bio7.compile.Model {

	private static final double EAST_MIN = 345000, EAST_MAX = 346000, NORTH_MIN = 5712000, NORTH_MAX = 5713000;
	private static final double ELEVATION_MIN = 100, ELEVATION_MAX = 400;
	private static final double BOX = 10, BOX_Z = 6;

	private double[][] sites;
	private int sphereList = -1;

	public DropLinesPlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		Random random = new Random(5);
		sites = new double[60][];
		for (int i = 0; i < sites.length; i++) {
			double east = EAST_MIN + random.nextDouble() * (EAST_MAX - EAST_MIN);
			double north = NORTH_MIN + random.nextDouble() * (NORTH_MAX - NORTH_MIN);
			/* A hill in the north east */
			double dx = (east - 345700) / 300, dy = (north - 5712700) / 300;
			double elevation = 140 + 220 * Math.exp(-(dx * dx + dy * dy) / 2) + 15 * random.nextGaussian();
			sites[i] = new double[] { east, north, Math.max(ELEVATION_MIN, Math.min(ELEVATION_MAX, elevation)) };
		}
		SpatialUtil.setDataBounds(0, BOX, 0, BOX, 0, BOX_Z);
		SpatialUtil.setAxisRanges(EAST_MIN, EAST_MAX, NORTH_MIN, NORTH_MAX, ELEVATION_MIN, ELEVATION_MAX);
		/* Whole meters instead of the scientific notation */
		SpatialUtil.setAxisNumberFormats("%.0f", "%.0f", null);
		SpatialUtil.setPlotView(true);
		SpatialUtil.setPlotGrids(true, true, true);
		SpatialUtil.setPlotTitle("Sampling Sites\nElevation with drop lines to the map");
		SpatialUtil.setAxisTitles("Easting [m]", "Northing [m]", "Elevation [m]");
		SpatialUtil.setColorBar("Elevation [m]", ELEVATION_MIN, ELEVATION_MAX, PlotLegend.TERRAIN);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private static double[] box(double[] site) {
		return new double[] { (site[0] - EAST_MIN) / (EAST_MAX - EAST_MIN) * BOX, (site[1] - NORTH_MIN) / (NORTH_MAX - NORTH_MIN) * BOX,
				(site[2] - ELEVATION_MIN) / (ELEVATION_MAX - ELEVATION_MIN) * BOX_Z };
	}

	/* The color of the elevation (the ramp of the color bar) */
	private static void color(GL2 gl, double elevation) {
		PlotLegend.setColor(gl, PlotLegend.rampColor(PlotLegend.TERRAIN, ELEVATION_MIN, ELEVATION_MAX, elevation));
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (sphereList == -1 || !gl.glIsList(sphereList)) {
			sphereList = gl.glGenLists(1);
			gl.glNewList(sphereList, GL_COMPILE);
			glut.glutSolidSphere(0.15, 14, 10);
			gl.glEndList();
		}
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT | GL_LINE_BIT | GL_COLOR_BUFFER_BIT);
		gl.glDisable(GL_LIGHTING);
		/* The drop lines (dashed) and the shadows on the floor */
		gl.glEnable(GL_LINE_STIPPLE);
		gl.glLineStipple(2, (short) 0xAAAA);
		gl.glColor3f(0.25f, 0.25f, 0.25f);
		gl.glBegin(GL_LINES);
		for (double[] site : sites) {
			double[] p = box(site);
			gl.glVertex3d(p[0], p[1], 0);
			gl.glVertex3d(p[0], p[1], p[2]);
		}
		gl.glEnd();
		gl.glDisable(GL_LINE_STIPPLE);
		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glColor4f(0.1f, 0.1f, 0.1f, 0.35f);
		for (double[] site : sites) {
			double[] p = box(site);
			gl.glBegin(GL_TRIANGLE_FAN);
			gl.glVertex3d(p[0], p[1], 0.01);
			for (int a = 0; a <= 16; a++) {
				double angle = 2 * Math.PI * a / 16;
				gl.glVertex3d(p[0] + 0.15 * Math.cos(angle), p[1] + 0.15 * Math.sin(angle), 0.01);
			}
			gl.glEnd();
		}
		gl.glDisable(GL_BLEND);
		/* The sites */
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL_NORMALIZE);
		for (double[] site : sites) {
			double[] p = box(site);
			color(gl, site[2]);
			gl.glPushMatrix();
			gl.glTranslated(p[0], p[1], p[2]);
			gl.glCallList(sphereList);
			gl.glPopMatrix();
		}
		gl.glPopAttrib();
	}

	public void close() {
		SpatialUtil.clearDataBounds();
		SpatialUtil.resetPlotLabels();
	}
}
