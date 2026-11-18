package opengl.plot3d;

import static com.jogamp.opengl.GL2.*;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A 3D vector field plot: the velocities of a rising vortex (e.g., the wind
 * of a thermal) on a regular grid as arrows. The length of the arrows is
 * proportional to the speed, the colors show the speed (blue slow, red fast).
 * The arrows rotate with the step rate of the model (the "Time" tab of the 3D
 * options).
 */
public class VectorFieldPlot3D extends com.eco.bio7.compile.Model {

	private static final int N = 9;
	private static final double MIN = -4, MAX = 4, Z_MIN = 0, Z_MAX = 6;
	private static final double MAX_SPEED = 1.8;

	private int coneList = -1;
	private double time = 0;

	public VectorFieldPlot3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		/* Removes the plot settings (title, axes, legends) of a former model */
		SpatialUtil.resetPlotLabels();
		SpatialUtil.setDataBounds(MIN, MAX, MIN, MAX, Z_MIN, Z_MAX);
		SpatialUtil.setPlotView(true);
		SpatialUtil.setPlotGrids(true, true, true);
		SpatialUtil.setPlotTitle("Vortex Flow Field\nArrows: velocity (colors: speed)");
		SpatialUtil.setAxisTitles("x [km]", "y [km]", "Height [km]");
		SpatialUtil.setColorBar("Speed [m/s]", 0, MAX_SPEED, PlotLegend.RAINBOW);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	/* The velocity of the vortex: a rotation around the z axis, an inflow at the bottom and an updraft */
	private double[] velocity(double x, double y, double z) {
		double r2 = x * x + y * y;
		double swirl = 1.2 * Math.exp(-r2 / 8);
		double inflow = -0.25 * (1 - z / Z_MAX) * Math.exp(-r2 / 12);
		double up = 1.0 * Math.exp(-r2 / 3) * (0.3 + z / Z_MAX);
		/* A slow oscillation of the rotation */
		double s = 1 + 0.3 * Math.sin(time);
		return new double[] { -y * swirl * s + x * inflow, x * swirl * s + y * inflow, up };
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (coneList == -1 || !gl.glIsList(coneList)) {
			coneList = gl.glGenLists(1);
			gl.glNewList(coneList, GL_COMPILE);
			glut.glutSolidCone(0.09, 0.25, 10, 2);
			gl.glEndList();
		}
		if (SpatialUtil.canStep()) {
			time += 0.05;
		}
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT | GL_LINE_BIT);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL_NORMALIZE);
		gl.glLineWidth(2);
		double scale = 0.55;
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				for (int k = 0; k < 5; k++) {
					double x = MIN + (MAX - MIN) * i / (N - 1);
					double y = MIN + (MAX - MIN) * j / (N - 1);
					double z = Z_MIN + 0.5 + (Z_MAX - Z_MIN - 1) * k / 4.0;
					double[] v = velocity(x, y, z);
					double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
					if (speed < 0.05) {
						continue;
					}
					PlotLegend.setColor(gl, PlotLegend.rampColor(PlotLegend.RAINBOW, 0, MAX_SPEED, speed));
					double ex = x + v[0] * scale, ey = y + v[1] * scale, ez = z + v[2] * scale;
					/* The shaft */
					gl.glDisable(GL_LIGHTING);
					gl.glBegin(GL_LINES);
					gl.glVertex3d(x, y, z);
					gl.glVertex3d(ex, ey, ez);
					gl.glEnd();
					/* The head: the cone (along +z) rotated in the direction of the velocity */
					gl.glEnable(GL_LIGHTING);
					gl.glPushMatrix();
					gl.glTranslated(ex, ey, ez);
					double angle = Math.toDegrees(Math.acos(v[2] / speed));
					gl.glRotated(angle, -v[1], v[0], 0);
					gl.glCallList(coneList);
					gl.glPopMatrix();
				}
			}
		}
		gl.glPopAttrib();
	}

	public void close() {
		SpatialUtil.clearDataBounds();
		SpatialUtil.resetPlotLabels();
	}
}
