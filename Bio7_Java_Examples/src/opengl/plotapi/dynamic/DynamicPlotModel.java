package opengl.plotapi.dynamic;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.HtmlSimulation;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.PlotRecorder;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A dynamic plot of a simulation: a Bio7 model (heat diffusion with a moving
 * heat source) updates a surface plot of the plot API in each step
 * (Plot3D.update, the camera and the view are kept). The steps are recorded
 * with a PlotRecorder: after 150 steps the recording is exported to an HTML
 * page (WebGL with Play/Pause and a timeline) and as an animation for Blender
 * (DynamicPlot.glb and DynamicPlot.py) in the home folder and the page is
 * opened in the web browser. The model starts automatically (the step rate in the "Time"
 * tab of the 3D options).
 */
public class DynamicPlotModel extends com.eco.bio7.compile.Model {

	private static final int N = 50, FRAMES = 150;
	private double[][] u = new double[N][N];
	private final double[] x = new double[N], y = new double[N];
	private Plot3D plot;
	private PlotRecorder recorder;
	private int t;

	public DynamicPlotModel() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		for (int i = 0; i < N; i++) {
			x[i] = i;
			y[i] = i;
		}
		u = new double[N][N];
		t = 0;
		/* Fixed ranges: the axes don't change with the data */
		plot = new Plot3D().title("Heat Diffusion\nStep 0").axisTitles("x", "y", "Temperature").zRange(0, 3)
				.textColors(0x1A237E, 0x37474F, 0x1A237E, 0x37474F).exportAction(true);
		plot.add(Plot3D.surface(x, y, u).colorMap(PlotLegend.RAINBOW)).show();
		recorder = new PlotRecorder().speed(15);
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	/* Called in each frame of the 3D view (the OpenGL thread) */
	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (!SpatialUtil.canStep()) {
			return;
		}
		step();
		/* The new data of the plot (the plot is drawn by the 3D view) */
		plot.title("Heat Diffusion\nStep " + t).clearLayers().add(Plot3D.surface(x, y, u).colorMap(PlotLegend.RAINBOW)).update();
		if (recorder.frames() < FRAMES) {
			recorder.frame(plot);
			if (recorder.frames() == FRAMES) {
				try {
					String file = System.getProperty("user.home") + "/DynamicPlot.html";
					recorder.export(file);
					/* The animation for Blender (a glTF file and a Python script, in Blender: Scripting - Open - Run Script) */
					String script = recorder.exportBlender(System.getProperty("user.home") + "/DynamicPlot.glb");
					System.out.println("Exported: " + file + " and for Blender: " + script);
					HtmlSimulation.open(file);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void step() {
		t++;
		double[][] next = new double[N][N];
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				double lap = (i > 0 ? u[i - 1][j] : 0) + (i < N - 1 ? u[i + 1][j] : 0) + (j > 0 ? u[i][j - 1] : 0) + (j < N - 1 ? u[i][j + 1] : 0) - 4 * u[i][j];
				next[i][j] = u[i][j] + 0.2 * lap - 0.01 * u[i][j];
			}
		}
		/* The heat source moves on a circle */
		int ci = (int) Math.round(N / 2 + N * 0.3 * Math.cos(t * 0.05)), cj = (int) Math.round(N / 2 + N * 0.3 * Math.sin(t * 0.05));
		for (int i = ci - 2; i <= ci + 2; i++) {
			for (int j = cj - 2; j <= cj + 2; j++) {
				if (i >= 0 && j >= 0 && i < N && j < N) {
					next[i][j] += 0.5;
				}
			}
		}
		u = next;
	}

	public void close() {
		Plot3D.clear();
	}
}
