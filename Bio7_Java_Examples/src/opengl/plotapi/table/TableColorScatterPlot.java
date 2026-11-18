package opengl.plotapi.table;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotTable;

/**
 * A 3D scatter plot of the Bio7 table colored by a fourth column (with a color
 * bar) and with drop lines to the floor. Demo data: TableDemoData (sheet
 * "Sites"). The title, the axes, the color map and the export action are set in
 * the dialog.
 */
public class TableColorScatterPlot {

	public static void main(String[] args) {
		PlotTable.Selection s = PlotTable.select("Colored 3D Scatter Plot", "Select the coordinates and the column of the colors.", "X", "Y", "Z",
				"Color");
		if (s == null) {
			return;
		}
		/* The settings of the dialog (title, axes, color map, export action) */
		s.apply(new Plot3D())
				.add(Plot3D.scatter(s.numbers(0), s.numbers(1), s.numbers(2)).values(s.numbers(3)).colorMap(s.colorMap()).dropLines(true).size(0.14))
				.colorBarTitle(s.name(3))
				.show();
	}
}
