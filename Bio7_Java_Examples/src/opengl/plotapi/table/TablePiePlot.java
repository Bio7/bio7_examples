package opengl.plotapi.table;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotTable;

/**
 * A 3D pie chart of the Bio7 table: a column of the labels (the legend) and a
 * column of the values (rows without a value >= 0 are ignored). Demo data:
 * TableDemoData (sheet "Carbon"). The title and the export action are set in
 * the dialog.
 */
public class TablePiePlot {

	public static void main(String[] args) {
		PlotTable.Selection s = PlotTable.select("3D Pie Chart", "Select the column of the labels and the column of the values.", "Labels", "Values");
		if (s == null) {
			return;
		}
		String[] labels = s.texts(0);
		double[] values = s.numbers(1);
		int n = 0;
		for (double v : values) {
			if (v >= 0) {
				n++;
			}
		}
		String[] l = new String[n];
		double[] v = new double[n];
		for (int i = 0, k = 0; i < values.length; i++) {
			if (values[i] >= 0) {
				l[k] = labels[i];
				v[k++] = values[i];
			}
		}
		/* The title and the export action of the dialog (a pie chart has no axes) */
		s.apply(new Plot3D())
				.add(Plot3D.pie(v, l).explode(0.12))
				.show();
	}
}
