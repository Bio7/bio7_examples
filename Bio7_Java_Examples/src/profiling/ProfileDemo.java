package profiling;

import java.util.Arrays;
import java.util.Random;

/*
 * An example for the Java profiler of Bio7: right-click in the editor,
 * Profile > Profile Java (compile and run). The report shows the time of the
 * lines and methods; the editor marks the slow lines.
 *
 * - sortValues(): the most time, spent in the JDK (Arrays.sort): the line of the
 *   call is marked, the flame graph shows the methods of the JDK below it
 * - simulate(): a nested loop (the JIT compiler often credits the time of a
 *   fast loop to its for line)
 * - neighbours(): a small method called very often (usually inlined by the JIT
 *   compiler: its time then belongs to simulate())
 * - Thread.sleep(): NOT in the profile (Java Flight Recorder counts CPU time;
 *   the run time in the report includes it)
 */
public class ProfileDemo {

	public static void main(String[] args) throws InterruptedException {
		double[][] grid = new double[400][400];
		Random random = new Random(1);
		for (double[] row : grid) {
			for (int i = 0; i < row.length; i++) {
				row[i] = random.nextDouble();
			}
		}
		double total = 0;
		for (int step = 0; step < 30; step++) {
			total += simulate(grid);
			total += sortValues(grid);
		}
		/* Waiting: not counted by the profiler */
		Thread.sleep(500);
		System.out.println("Result: " + total);
	}

	/* A diffusion step */
	static double simulate(double[][] grid) {
		int n = grid.length;
		double[][] next = new double[n][n];
		double sum = 0;
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				double v = 0.6 * grid[y][x] + 0.1 * neighbours(grid, x, y);
				next[y][x] = Math.exp(-v) * Math.sin(v) + v * 0.5;
				sum += next[y][x];
			}
		}
		for (int y = 0; y < n; y++) {
			grid[y] = next[y];
		}
		return sum;
	}

	/* Called 160,000 times per step */
	static double neighbours(double[][] g, int x, int y) {
		int n = g.length;
		return g[(y + 1) % n][x] + g[(y + n - 1) % n][x] + g[y][(x + 1) % n] + g[y][(x + n - 1) % n];
	}

	/* Time in the JDK: sorting the values of the grid */
	static double sortValues(double[][] grid) {
		double[] all = Arrays.stream(grid).flatMapToDouble(Arrays::stream).toArray();
		Arrays.sort(all);
		return all[all.length / 2];
	}
}
