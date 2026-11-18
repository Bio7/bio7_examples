/*
This example plots spheres from the selection of the active Bio7 table sheet
in the 3D view (the 3D perspective is opened).

Select the cells in the table (open the Table view): one row per sphere with
the columns x, y, z and diameter. If the header of the table has the names
x, y, z and diameter (or d, size) the columns are found by the names, else the
first four selected columns are used (without a fourth column the diameter is
the default diameter below). Rows with values which are not numbers (e.g., a
header row) are skipped.

The spheres are colored by the diameter (blue small, green, yellow large).
By default the spheres are shown in a plot view (like a 3D plot): the box, the
grids behind the data, the axes with the numbers and the camera are fitted to
the spheres. SpatialUtil.clearDataBounds() removes the fitting again.
*/
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import com.eco.bio7.collection.Work;
import com.eco.bio7.compile.*;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.rbridge.Bio7Grid;
import com.eco.bio7.spatial.SpatialUtil;
import org.eclipse.swt.widgets.Display;

/* The settings */
def defaultDiameter = 10.0
def scaleCoordinates = 1.0 // A factor for x, y, z
def scaleDiameter = 1.0 // A factor for the diameter
def plotView = true // The plot view fitted to the spheres
def padding = 0.05 // The space around the spheres (a fraction of the size)

/* Reads the selected rows of the active sheet (in the SWT thread) */
def readSelection = {
	def spheres = []
	def message = null
	Display.getDefault().syncExec({
		def grid = Bio7Grid.getGrid()
		if (grid == null || grid.isDisposed()) {
			message = "No table sheet available! Open the Table view and select the values."
			return
		}
		def cells = grid.getCellSelection()
		if (cells == null || cells.length == 0) {
			message = "No cells selected in the table!"
			return
		}
		def selectedColumns = cells.collect { it.x }.unique().sort()
		def selectedRows = cells.collect { it.y }.unique().sort()
		/* The columns by the header names or the selected columns in their order */
		def names = (0..<grid.getColumnCount()).collect { grid.getColumn(it).getText().trim().toLowerCase() }
		def find = { List<String> keys -> names.findIndexOf { it in keys } }
		def columns = [find(["x"]), find(["y"]), find(["z"]), find(["diameter", "d", "size"])]
		if (columns[0] < 0 || columns[1] < 0 || columns[2] < 0) {
			columns = selectedColumns.take(4)
			if (columns.size() < 3) {
				message = "Select at least three columns (x, y, z and optional the diameter)!"
				return
			}
		}
		selectedRows.each { row ->
			def item = grid.getItem(row)
			def values = columns.collect { c -> c >= 0 && c < grid.getColumnCount() ? item.getText(c).trim().replace(',', '.') : "" }
			try {
				def x = Double.parseDouble(values[0]), y = Double.parseDouble(values[1]), z = Double.parseDouble(values[2])
				def d = values.size() > 3 && values[3] ? Double.parseDouble(values[3]) : defaultDiameter
				spheres << [x * scaleCoordinates, y * scaleCoordinates, z * scaleCoordinates, d * scaleDiameter]
			} catch (NumberFormatException e) {
				/* A header row or an empty cell */
			}
		}
	} as Runnable)
	if (message != null) {
		println message
	}
	return spheres
}

class SpheresModel extends com.eco.bio7.compile.Model {

	def spheres = []
	def minD = 0, maxD = 1

	void setSpheres(List s) {
		spheres = s
		if (!s.isEmpty()) {
			minD = s.collect { it[3] }.min()
			maxD = s.collect { it[3] }.max()
		}
	}

	/* A color ramp: blue (0) - green (0.5) - yellow (1) */
	static double[] color(double t) {
		double[] blue = [0.15d, 0.35d, 0.95d], green = [0.2d, 0.8d, 0.35d], yellow = [1.0d, 0.9d, 0.1d];
		double[] a = t < 0.5d ? blue : green, b = t < 0.5d ? green : yellow;
		double f = t < 0.5d ? t * 2d : (t - 0.5d) * 2d;
		return [a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f] as double[];
	}

	void run(GL2 gl, GLU glu, GLUT glut) {
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL2.GL_NORMALIZE);
		for (s in spheres) {
			/* The color by the diameter: blue - green - yellow */
			/* Groovy computes with double values (glColor3d) */
			double t = maxD > minD ? (s[3] - minD) / (maxD - minD) : 0.5d;
			double[] c = color(t);
			gl.glColor3d(c[0], c[1], c[2]);
			gl.glPushMatrix();
			gl.glTranslated(s[0], s[1], s[2]);
			glut.glutSolidSphere(s[3] / 2.0, 24, 16);
			gl.glPopMatrix();
		}
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
	}
}

def spheres = readSelection()
if (spheres.isEmpty()) {
	println "No spheres to plot!"
	return
}
Work.openPerspective("com.eco.bio7.perspective_3d");
def model = new SpheresModel()
model.setSpheres(spheres)
Compiled.setModel(model)
/* The box, the grids, the axes and the camera fitted to the spheres (with the radius) */
def bounds = (0..2).collect { k -> [spheres.collect { it[k] - it[3] / 2 }.min(), spheres.collect { it[k] + it[3] / 2 }.max()] }
def size = bounds.collect { it[1] - it[0] }.max()
def pad = Math.max(size * padding, 1e-9)
SpatialUtil.setDataBounds(bounds[0][0] - pad, bounds[0][1] + pad, bounds[1][0] - pad, bounds[1][1] + pad,
		bounds[2][0] - pad, bounds[2][1] + pad)
SpatialUtil.setPlotView(plotView)
/* The floor (XY) and the walls (XZ, YZ) of the plot view */
SpatialUtil.setPlotGrids(true, true, true)
/* Start the drawing loop of the compiled model (the run method) */
if (SpatialUtil.isStarted() == false) {
	SpatialUtil.startStop();
}
println "Plotted " + spheres.size() + " spheres"
