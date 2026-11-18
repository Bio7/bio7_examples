package opengl.plotapi.table;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotTable;

/**
 * A 3D bar plot of the Bio7 table: a bar for each row at the position x, y with
 * the height of a third column (e.g., counts of classes). Demo data:
 * TableDemoData (sheet "Ages"). The title, the axes, the color map and the
 * export action are set in the dialog.
 */
public class TableBarPlot {

	public static void main(String[] args) {
		PlotTable.Selection s = PlotTable.select("3D Bar Plot", "Select the positions of the bars and the column of the heights.", "X", "Y",
				"Height");
		if (s == null) {
			return;
		}
		s.apply(new Plot3D())
				.add(Plot3D.bars(s.numbers(0), s.numbers(1), s.numbers(2)).colorMap(s.colorMap()).width(0.8))
				.colorBarTitle(s.name(2))
				.show();
	}
}
