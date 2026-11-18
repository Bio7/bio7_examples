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
 * A 3D scatter plot in the plot view of the 3D view: three clusters of
 * normally distributed points (e.g., the measurements of three species) as
 * spheres with the colors of the clusters.
 * <p>
 * The data is drawn in its own coordinates. SpatialUtil.setDataBounds tells
 * the 3D view the range of the data: the view fits the data into the camera
 * and the plot view displays the grids and the axes with the numbers of the
 * data. Rotate the plot with the mouse. The model is started automatically
 * (the run method is called while the model runs).
 */
public class ScatterPlot3D extends com.eco.bio7.compile.Model {

	private static final int POINTS_PER_CLUSTER = 300;
	/* The centers and the standard deviations of the clusters (x, y, z) */
	private final double[][] centers = { { 2, 2, 3 }, { 6, 7, 5 }, { 7, 3, 8 } };
	private final double[][] sd = { { 0.8, 0.8, 0.6 }, { 1.0, 0.7, 1.0 }, { 0.6, 1.1, 0.8 } };
	/* The colors of the clusters */
	private final int[] colors = { 0x1F77B4, 0xFF7F0E, 0x2CA02C };

	private double[][] points;
	private int[] cluster;
	private int sphereList = -1;

	public ScatterPlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		createData();
		/* The range of the data for the camera, the grids and the axis numbers */
		SpatialUtil.setDataBounds(0, 10, 0, 10, 0, 10);
		SpatialUtil.setPlotView(true);
		/* The floor grid, too (no height map in this plot) */
		SpatialUtil.setPlotGrids(true, true, true);
		/* The title and the titles of the axes */
		SpatialUtil.setPlotTitle("Three Clusters\nMeasurements of three species (n = 900)");
		SpatialUtil.setAxisTitles("Length [cm]", "Width [cm]", "Height [cm]");
		/* The legend with the colors of the clusters */
		SpatialUtil.setLegend("Species", new String[] { "Species A", "Species B", "Species C" }, colors);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private void createData() {
		Random random = new Random(42);
		int n = centers.length * POINTS_PER_CLUSTER;
		points = new double[n][3];
		cluster = new int[n];
		for (int i = 0; i < n; i++) {
			int c = i / POINTS_PER_CLUSTER;
			cluster[i] = c;
			for (int k = 0; k < 3; k++) {
				/* Clamp the values to the plot box */
				points[i][k] = Math.max(0, Math.min(10, centers[c][k] + random.nextGaussian() * sd[c][k]));
			}
		}
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		/* A display list for the sphere (created again in a new OpenGL context) */
		if (sphereList == -1 || !gl.glIsList(sphereList)) {
			sphereList = gl.glGenLists(1);
			gl.glNewList(sphereList, GL_COMPILE);
			glut.glutSolidSphere(0.08, 10, 8);
			gl.glEndList();
		}
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT);
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		/* The data is scaled into the view: normalize the normals for the light */
		gl.glEnable(GL_NORMALIZE);
		for (int i = 0; i < points.length; i++) {
			PlotLegend.setColor(gl, colors[cluster[i]]);
			gl.glPushMatrix();
			gl.glTranslated(points[i][0], points[i][1], points[i][2]);
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
