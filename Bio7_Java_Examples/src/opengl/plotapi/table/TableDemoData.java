package opengl.plotapi.table;

import java.util.Locale;
import java.util.Random;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.rbridge.Bio7Grid;
import com.eco.bio7.rbridge.RTable;

/**
 * Creates sheets with demo data in the Bio7 table for the table plot examples
 * (the first row contains the names of the columns): "Species" (scatter plot),
 * "Sites" (colored scatter plot), "Grid" (surface), "Ages" (bar plot) and
 * "Carbon" (pie chart). Run it first, then a table plot example and select the
 * columns in the dialog.
 */
public class TableDemoData {

	public static void main(String[] args) {
		/* Opens the Bio7 table */
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(RTable.ID);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		Random random = new Random(3);
		/* Three species: length, width, height */
		double[][] centers = { { 4.5, 3.2, 1.5 }, { 6.0, 2.8, 4.3 }, { 6.6, 3.0, 5.6 } };
		String[][] species = new String[1 + 150][];
		species[0] = new String[] { "Length", "Width", "Height", "Species" };
		for (int i = 0; i < 150; i++) {
			int g = i / 50;
			species[i + 1] = new String[] { n(centers[g][0] + 0.4 * random.nextGaussian()), n(centers[g][1] + 0.3 * random.nextGaussian()),
					n(centers[g][2] + 0.4 * random.nextGaussian()), "Species " + "ABC".charAt(g) };
		}
		Bio7Grid.setValues(species, "Species");
		/* Sampling sites: easting, northing, elevation, temperature */
		String[][] sites = new String[1 + 120][];
		sites[0] = new String[] { "Easting", "Northing", "Elevation", "Temperature" };
		for (int i = 0; i < 120; i++) {
			double e = random.nextDouble() * 1000, no = random.nextDouble() * 1000;
			double dx = (e - 650) / 250, dy = (no - 600) / 250;
			double elevation = 150 + 300 * Math.exp(-(dx * dx + dy * dy) / 2) + 10 * random.nextGaussian();
			sites[i + 1] = new String[] { n(e), n(no), n(elevation), n(14 - 0.01 * elevation + 0.8 * random.nextGaussian()) };
		}
		Bio7Grid.setValues(sites, "Sites");
		/* A regular grid (long format): x, y, z */
		int size = 30;
		String[][] grid = new String[1 + size * size][];
		grid[0] = new String[] { "x", "y", "z" };
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				double x = -3 + 6.0 * i / (size - 1), y = -3 + 6.0 * j / (size - 1);
				grid[1 + i * size + j] = new String[] { n(x), n(y), n(Math.sin(x) * Math.cos(y) * Math.exp(-(x * x + y * y) / 8)) };
			}
		}
		Bio7Grid.setValues(grid, "Grid");
		/* Age classes over the years: age, year, individuals */
		String[][] ages = new String[1 + 10 * 8][];
		ages[0] = new String[] { "Age", "Year", "Individuals" };
		int k = 1;
		for (int a = 0; a < 10; a++) {
			for (int y = 0; y < 8; y++) {
				double peak = 30 + 3 * y;
				ages[k++] = new String[] { n(5 + 10 * a), n(1990 + 5 * y),
						n(Math.round(1000 * Math.exp(-Math.pow((5 + 10 * a - peak) / 28, 2)) * (1 - 0.03 * y) + 150)) };
			}
		}
		Bio7Grid.setValues(ages, "Ages");
		/* The carbon pools of a forest */
		Bio7Grid.setValues(new String[][] { { "Pool", "Carbon" }, { "Soil", "42" }, { "Stems", "23" }, { "Roots", "18" }, { "Litter", "9" },
				{ "Branches", "5" }, { "Leaves", "3" } }, "Carbon");
		System.out.println("Created the sheets Species, Sites, Grid, Ages and Carbon in the Bio7 table.");
	}

	private static String n(double v) {
		return String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
	}
}
