package opengl.plotapi.dynamic;

import java.util.Random;

import com.eco.bio7.spatial.HtmlSimulation;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotRecorder;

/**
 * Records a plot of a simulation without the 3D view: random walkers of two
 * species which drift to two centers (a scatter plot for each step). The
 * PlotRecorder exports the recording to an HTML page with WebGL (Play/Pause,
 * timeline, speed, loop) and as an animation for Blender (RandomWalkers.glb and
 * RandomWalkers.py) in the home folder and opens the page in the web browser.
 */
public class RecordPlotApi {

	public static void main(String[] args) throws Exception {
		Random random = new Random(1);
		int n = 150, steps = 120;
		double[][] x = new double[2][n], y = new double[2][n], z = new double[2][n];
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i < n; i++) {
				x[s][i] = random.nextDouble() * 100;
				y[s][i] = random.nextDouble() * 100;
				z[s][i] = random.nextDouble() * 100;
			}
		}
		double[][] centers = { { 30, 30, 60 }, { 70, 70, 40 } };
		Plot3D plot = new Plot3D().axisTitles("x [m]", "y [m]", "z [m]").xRange(0, 100).yRange(0, 100).zRange(0, 100);
		PlotRecorder recorder = new PlotRecorder().speed(20).autoplay(true);
		for (int t = 0; t < steps; t++) {
			for (int s = 0; s < 2; s++) {
				for (int i = 0; i < n; i++) {
					x[s][i] = clamp(x[s][i] + random.nextGaussian() * 2 + (centers[s][0] - x[s][i]) * 0.03);
					y[s][i] = clamp(y[s][i] + random.nextGaussian() * 2 + (centers[s][1] - y[s][i]) * 0.03);
					z[s][i] = clamp(z[s][i] + random.nextGaussian() * 2 + (centers[s][2] - z[s][i]) * 0.03);
				}
			}
			plot.title("Random Walkers\nStep " + t).clearLayers().add(Plot3D.scatter(x[0], y[0], z[0]).label("Species A").size(0.12))
					.add(Plot3D.scatter(x[1], y[1], z[1]).label("Species B").size(0.12));
			recorder.frame(plot);
		}
		String file = System.getProperty("user.home") + "/RandomWalkers.html";
		recorder.export(file);
		/* The animation for Blender (a glTF file and a Python script, in Blender: Scripting - Open - Run Script) */
		String script = recorder.exportBlender(System.getProperty("user.home") + "/RandomWalkers.glb");
		System.out.println("Exported " + recorder.frames() + " frames: " + file + " and for Blender: " + script);
		HtmlSimulation.open(file);
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(100, v));
	}
}
