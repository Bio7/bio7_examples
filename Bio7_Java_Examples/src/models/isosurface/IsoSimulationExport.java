/*
 * ISO SURFACE: EXPORT OF A SIMULATION AS A TIME SERIES (SIMULATION + EXPORT)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. The simulation: A tumor or a bacterial colony grows from a seed. Cells
 *    divide where enough nutrients are available. The nutrients diffuse from
 *    the borders of the volume and are consumed by the cells. The colony
 *    becomes a branched structure (nutrient limited growth).
 *
 * 2. The export: Every EXPORT_EVERY time steps the iso surface of the cell
 *    density is computed (computeNow) and exported as a numbered file
 *    (colony_0001.vtk, ...) to the folder "Bio7_IsoSurface/colony" in the home
 *    directory. While a frame is exported the simulation waits.
 *
 *    - VTK files can be opened as a time series in ParaView (File > Open, the
 *      numbered files are grouped).
 *    - Change FORMAT to "ply", "glb", "obj" or "stl" for other programs
 *      (e.g., Blender, MeshLab).
 *
 * 3. The surface is colored by the depth (COLOR_DEPTH). The colors are also
 *    exported (VTK, PLY, glTF and OBJ).
 */

package models.isosurface;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoSimulationExport extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 64;
	private static final int NN = N * N;
	// Nutrient diffusion, consumption and the division threshold
	private static final float D = 0.15f;
	private static final float CONSUMPTION = 0.04f;
	private static final float DIVISION_THRESHOLD = 0.35f;
	private static final float DIVISION_PROBABILITY = 0.2f;
	// Model iterations per time step of Bio7
	private static final int ITERATIONS_PER_STEP = 4;
	// Export settings
	private static final int EXPORT_EVERY = 5;
	private static final int MAX_FRAMES = 40;
	private static final String FORMAT = "vtk";

	private final float[] cells = new float[N * NN];
	private float[] nutrient = new float[N * NN];
	private float[] nutrientNext = new float[N * NN];
	private final Random random = new Random();
	private IsoSurface iso;
	private int step;
	private int frame;
	private volatile boolean exporting;
	private File folder;

	/* Start after compilation if run action is active! */
	public IsoSimulationExport() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		java.util.Arrays.fill(cells, 0f);
		java.util.Arrays.fill(nutrient, 1f);
		// The seed in the center
		for (int z = N / 2 - 2; z <= N / 2 + 2; z++) {
			for (int y = N / 2 - 2; y <= N / 2 + 2; y++) {
				for (int x = N / 2 - 2; x <= N / 2 + 2; x++) {
					cells[x + y * N + z * NN] = 1f;
				}
			}
		}
		folder = new File(System.getProperty("user.home"), "Bio7_IsoSurface/colony");
		folder.mkdirs();
		step = 0;
		frame = 0;
		exporting = false;
		iso = null;
		/* Start the run method! */
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setup();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (iso == null) {
			iso = SpatialUtil.getIsoSurface();
			if (iso == null) {
				return;
			}
			// Start with the default colors (the iso surface keeps the settings of a former model)
			iso.resetColors();
			iso.setIsoValue(0.5);
			iso.setSmoothing(0.8f);
			iso.setMeshSmoothing(3);
			iso.setOpacity(1f);
			iso.setColorMode(IsoSurface.COLOR_DEPTH);
			iso.setData(cells, N, N, N);
		}
		// The simulation waits while a frame is exported
		if (!SpatialUtil.canStep() || exporting || frame >= MAX_FRAMES) {
			return;
		}
		for (int k = 0; k < ITERATIONS_PER_STEP; k++) {
			diffuseNutrient();
			divideCells();
		}
		step++;
		iso.setData(cells, N, N, N);
		if (step % EXPORT_EVERY == 0) {
			exportFrame();
		}
	}

	/* Computes the surface of the current data and exports it in a background thread! */
	private void exportFrame() {
		exporting = true;
		frame++;
		final File file = new File(folder, String.format("colony_%04d.%s", frame, FORMAT));
		new Thread(() -> {
			try {
				iso.computeNow();
				iso.export(file);
				System.out.println("Exported " + file.getName() + ": " + iso.getInfo());
			} catch (IOException e) {
				System.err.println("Export failed: " + e.getMessage());
			} finally {
				exporting = false;
			}
		}).start();
	}

	/* Nutrient diffusion (1 at the borders) and consumption by the cells! */
	private void diffuseNutrient() {
		IntStream.range(1, N - 1).parallel().forEach(z -> {
			for (int y = 1; y < N - 1; y++) {
				for (int x = 1; x < N - 1; x++) {
					int i = x + y * N + z * NN;
					float lap = nutrient[i - 1] + nutrient[i + 1] + nutrient[i - N] + nutrient[i + N]
							+ nutrient[i - NN] + nutrient[i + NN] - 6 * nutrient[i];
					nutrientNext[i] = Math.max(0, nutrient[i] + D * lap - CONSUMPTION * cells[i] * nutrient[i]);
				}
			}
		});
		float[] swap = nutrient;
		nutrient = nutrientNext;
		nutrientNext = swap;
	}

	/* Cells divide into a free neighbor if enough nutrients are available! */
	private void divideCells() {
		int[] offsets = { 1, -1, N, -N, NN, -NN };
		for (int z = 1; z < N - 1; z++) {
			for (int y = 1; y < N - 1; y++) {
				for (int x = 1; x < N - 1; x++) {
					int i = x + y * N + z * NN;
					if (cells[i] > 0 && nutrient[i] > DIVISION_THRESHOLD
							&& random.nextFloat() < DIVISION_PROBABILITY * nutrient[i]) {
						int j = i + offsets[random.nextInt(6)];
						if (cells[j] == 0) {
							cells[j] = 1f;
						}
					}
				}
			}
		}
	}
}
