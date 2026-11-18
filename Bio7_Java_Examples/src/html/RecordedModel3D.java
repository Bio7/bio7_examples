package html;

import static com.jogamp.opengl.GL2.*;

import java.util.Random;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.HtmlSimulation;
import com.eco.bio7.spatial.SimulationRecorder;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * Records a running Bio7 model (OpenGL in the 3D view): random walkers which
 * are attracted by the center. Each step of the model (SpatialUtil.canStep) is
 * drawn in the 3D view and recorded. After 300 steps the recording is exported
 * to an HTML page (in the home folder) and opened in the web browser. Start
 * the model in the 3D view (it starts automatically).
 */
public class RecordedModel3D extends com.eco.bio7.compile.Model {

	private static final int N = 200, FRAMES = 300;
	private final double[] x = new double[N], y = new double[N], z = new double[N];
	private final Random random = new Random(4);
	private SimulationRecorder recorder;
	private boolean exported;
	private int sphereList = -1;

	public RecordedModel3D() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		SpatialUtil.resetPlotLabels();
		/* The model coordinates 0 - 100 (the box of the 3D view) */
		SpatialUtil.setDataBounds(0, 100, 0, 100, 0, 100);
		for (int i = 0; i < N; i++) {
			x[i] = random.nextDouble() * 100;
			y[i] = random.nextDouble() * 100;
			z[i] = random.nextDouble() * 100;
		}
		/* The recording: the box of the page has the size 1 (the positions are scaled) */
		recorder = new SimulationRecorder("Random Walkers (a recorded Bio7 model)").view(HtmlSimulation.View.SCENE_3D).points(0.012, 0xD62728)
				.chart("Mean distance to the center").speed(30);
		exported = false;
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (SpatialUtil.canStep()) {
			step();
		}
		if (sphereList == -1 || !gl.glIsList(sphereList)) {
			sphereList = gl.glGenLists(1);
			gl.glNewList(sphereList, GL_COMPILE);
			glut.glutSolidSphere(1.2, 10, 8);
			gl.glEndList();
		}
		gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT);
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glEnable(GL_NORMALIZE);
		gl.glColor3f(0.84f, 0.15f, 0.16f);
		for (int i = 0; i < N; i++) {
			gl.glPushMatrix();
			gl.glTranslated(x[i], y[i], z[i]);
			gl.glCallList(sphereList);
			gl.glPopMatrix();
		}
		gl.glPopAttrib();
	}

	/* One step of the model and its recording */
	private void step() {
		double distance = 0;
		double[] px = new double[N], py = new double[N], pz = new double[N];
		for (int i = 0; i < N; i++) {
			x[i] = clamp(x[i] + random.nextGaussian() * 2 + (50 - x[i]) * 0.01);
			y[i] = clamp(y[i] + random.nextGaussian() * 2 + (50 - y[i]) * 0.01);
			z[i] = clamp(z[i] + random.nextGaussian() * 2 + (50 - z[i]) * 0.01);
			distance += Math.sqrt((x[i] - 50) * (x[i] - 50) + (y[i] - 50) * (y[i] - 50) + (z[i] - 50) * (z[i] - 50));
			px[i] = x[i] / 100;
			py[i] = y[i] / 100;
			pz[i] = z[i] / 100;
		}
		if (recorder.frames() < FRAMES) {
			recorder.frame().points(px, py, pz).chart("Distance", distance / N);
		} else if (!exported) {
			exported = true;
			try {
				String file = System.getProperty("user.home") + "/RecordedModel3D.html";
				recorder.export(file);
				System.out.println("Exported: " + file);
				HtmlSimulation.open(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(100, v));
	}

	public void close() {
		SpatialUtil.clearDataBounds();
	}
}
