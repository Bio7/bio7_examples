package html;

import java.util.Random;

import com.eco.bio7.spatial.HtmlSimulation;
import com.eco.bio7.spatial.SimulationRecorder;

/**
 * The Game of Life computed in Java and recorded step by step
 * (SimulationRecorder). The exported HTML page plays the recording: Play/Pause,
 * the speed, a timeline to jump to a step, a loop and the chart of the
 * population.
 */
public class RecordedGameOfLife {

	public static SimulationRecorder record(int steps) {
		int w = 150, h = 150;
		byte[] cells = new byte[w * h], next = new byte[w * h];
		Random random = new Random(1);
		for (int i = 0; i < cells.length; i++) {
			cells[i] = (byte) (random.nextDouble() < 0.3 ? 1 : 0);
		}
		SimulationRecorder recorder = new SimulationRecorder("Game of Life (recorded in Java)")
				.description("The model was computed in Java, the page plays the recorded steps.").view(HtmlSimulation.View.GRID_2D)
				.palette(0xFFFFFF, 0x1F3A93).chart("Population").speed(15);
		for (int t = 0; t < steps; t++) {
			int alive = 0;
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int n = 0;
					for (int dy = -1; dy <= 1; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							if (dx != 0 || dy != 0) {
								n += cells[((y + dy + h) % h) * w + (x + dx + w) % w];
							}
						}
					}
					int state = cells[y * w + x] == 1 ? (n == 2 || n == 3 ? 1 : 0) : (n == 3 ? 1 : 0);
					next[y * w + x] = (byte) state;
					alive += state;
				}
			}
			byte[] swap = cells;
			cells = next;
			next = swap;
			/* A frame for each step: the grid and the value of the chart */
			recorder.frame().grid(w, h, cells).chart("Population", alive);
		}
		return recorder;
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/RecordedGameOfLife.html";
		record(400).export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
