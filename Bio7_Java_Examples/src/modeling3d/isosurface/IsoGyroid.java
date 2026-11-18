/*
 * ISO SURFACE: GYROID LATTICE (GEOMETRY + EXPORT)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the iso surface of a gyroid lattice is displayed.
 *
 * --- HOW THE CODE WORKS ---
 * 1. The gyroid is a triply periodic minimal surface:
 *       sin(x)cos(y) + sin(y)cos(z) + sin(z)cos(x) = 0
 *    A sheet with a thickness is created with the field THICKNESS - |f|
 *    and the iso value 0 (two periods in every direction).
 *
 * 2. The field is handed to the iso surface of the 3D view (setData). The
 *    surface is closed at the borders of the volume (printable).
 *
 * 3. Export: The surface is computed (computeNow) and exported as STL (3D
 *    printing) and glTF binary (Blender, web viewers) to the folder
 *    "Bio7_IsoSurface" in the home directory. The export runs in a background
 *    thread.
 */

package modeling3d.isosurface;

import java.io.File;
import java.io.IOException;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoGyroid extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 128;
	// The number of periods in every direction
	private static final double PERIODS = 2;
	// The thickness of the sheet (0 - 1.5)
	private static final double THICKNESS = 0.35;
	// Export the surface (STL and glTF)?
	private static final boolean EXPORT = true;

	private boolean created;

	/* Start after compilation if run action is active! */
	public IsoGyroid() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		created = false;
		/* Start the run method! */
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setup();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (created) {
			return;
		}
		// The 3D view is available now
		IsoSurface iso = SpatialUtil.getIsoSurface();
		if (iso == null) {
			return;
		}
		// Start with the default colors (the iso surface keeps the settings of a former model)
		iso.resetColors();
		created = true;
		float[] field = new float[N * N * N];
		double scale = PERIODS * 2 * Math.PI / (N - 1);
		for (int z = 0; z < N; z++) {
			for (int y = 0; y < N; y++) {
				for (int x = 0; x < N; x++) {
					double px = x * scale, py = y * scale, pz = z * scale;
					double f = Math.sin(px) * Math.cos(py) + Math.sin(py) * Math.cos(pz) + Math.sin(pz) * Math.cos(px);
					field[x + y * N + z * N * N] = (float) (THICKNESS - Math.abs(f));
				}
			}
		}
		iso.setIsoValue(0);
		iso.setSmoothing(0.5f);
		iso.setMeshSmoothing(3);
		iso.setOpacity(1f);
		iso.setColorMode(IsoSurface.COLOR_DEPTH);
		iso.setData(field, N, N, N);
		if (EXPORT) {
			/* Compute the surface and export it (in the background)! */
			new Thread(() -> {
				iso.computeNow();
				File dir = new File(System.getProperty("user.home"), "Bio7_IsoSurface");
				dir.mkdirs();
				try {
					iso.export(new File(dir, "gyroid.stl"));
					iso.export(new File(dir, "gyroid.glb"));
					System.out.println("Exported: " + iso.getInfo() + " to " + dir);
				} catch (IOException e) {
					System.err.println("Export failed: " + e.getMessage());
				}
			}).start();
		}
	}
}
