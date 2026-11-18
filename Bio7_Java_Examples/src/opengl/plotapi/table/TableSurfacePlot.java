package opengl.plotapi.table;

import java.util.Arrays;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotTable;

/**
 * A surface of three columns of the Bio7 table with the values of a regular
 * grid in the long format (one row for each x, y and z). The grid is created
 * from the different x and y values (missing values are gaps), the contour
 * lines are displayed on the floor. Demo data: TableDemoData (sheet "Grid").
 * The title, the axes, the color map and the export action are set in the
 * dialog.
 */
public class TableSurfacePlot {

	public static void main(String[] args) {
		PlotTable.Selection s = PlotTable.select("Surface Plot", "Select the columns of a regular grid (x, y and the value z).", "X", "Y", "Z");
		if (s == null) {
			return;
		}
		double[] x = s.numbers(0), y = s.numbers(1), z = s.numbers(2);
		/* The different x and y values (sorted) are the grid */
		double[] xs = Arrays.stream(x).filter(Double::isFinite).sorted().distinct().toArray();
		double[] ys = Arrays.stream(y).filter(Double::isFinite).sorted().distinct().toArray();
		if (xs.length < 2 || ys.length < 2) {
			System.err.println("A surface needs at least two different x and y values.");
			return;
		}
		double[][] grid = new double[xs.length][ys.length];
		for (double[] row : grid) {
			Arrays.fill(row, Double.NaN);
		}
		for (int i = 0; i < x.length; i++) {
			int a = Arrays.binarySearch(xs, x[i]), b = Arrays.binarySearch(ys, y[i]);
			if (a >= 0 && b >= 0) {
				grid[a][b] = z[i];
			}
		}
		s.apply(new Plot3D())
				.add(Plot3D.surface(xs, ys, grid).colorMap(s.colorMap()))
				.add(Plot3D.contour(xs, ys, grid).levels(10).onFloor(true).colorMap(s.colorMap()))
				.colorBarTitle(s.name(2))
				.show();
	}
}
