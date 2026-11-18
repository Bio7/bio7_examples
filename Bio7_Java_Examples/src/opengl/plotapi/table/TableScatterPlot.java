package opengl.plotapi.table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotTable;

/**
 * A 3D scatter plot of three columns of the active sheet of the Bio7 table
 * (selected in a dialog) with an optional column of groups (a layer and a
 * legend entry for each group). The title, the axis titles, the log axes and
 * the export action are set in the dialog, too. Demo data: TableDemoData
 * (sheet "Species").
 */
public class TableScatterPlot {

	public static void main(String[] args) {
		PlotTable.Selection s = PlotTable.select("3D Scatter Plot", "Select the columns of the coordinates and optional the groups.", "X", "Y", "Z",
				"Group?");
		if (s == null) {
			return;
		}
		double[] x = s.numbers(0), y = s.numbers(1), z = s.numbers(2);
		/* The title, the axis titles, the log axes and the export action of the dialog */
		Plot3D plot = s.apply(new Plot3D());
		if (s.has(3)) {
			/* The rows of each group */
			String[] group = s.texts(3);
			Map<String, List<Integer>> rows = new LinkedHashMap<>();
			for (int i = 0; i < group.length; i++) {
				rows.computeIfAbsent(group[i].isEmpty() ? "(empty)" : group[i], _ -> new ArrayList<>()).add(i);
			}
			for (Map.Entry<String, List<Integer>> e : rows.entrySet()) {
				int n = e.getValue().size();
				double[] gx = new double[n], gy = new double[n], gz = new double[n];
				for (int k = 0; k < n; k++) {
					int i = e.getValue().get(k);
					gx[k] = x[i];
					gy[k] = y[i];
					gz[k] = z[i];
				}
				plot.add(Plot3D.scatter(gx, gy, gz).label(e.getKey()));
			}
		} else {
			plot.add(Plot3D.scatter(x, y, z).label("Data"));
		}
		plot.show();
	}
}
