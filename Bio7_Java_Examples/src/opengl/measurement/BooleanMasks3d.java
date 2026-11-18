package opengl.measurement;

import java.util.Locale;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.Measurement3d;
import com.eco.bio7.spatial.SpatialUtil;
import com.eco.bio7.spatial.ValueSelection;
import com.eco.bio7.spatial.VolumeRenderer;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Boolean operations of 3D masks with the Image Calculator of ImageJ and the
 * measurement of the result in the 3D perspective.
 * <p>
 * Example: the volume of a defect (e.g., a bone defect after a resection). The
 * healthy side is mirrored onto the defect side and the defect side is
 * subtracted: Mirrored - Original leaves exactly the missing volume, which is
 * then displayed and measured as an iso surface (Measure tab: volume, surface
 * area, ...).
 * <p>
 * The test stack is created here: two balls (radius 20 voxels), symmetric to
 * the center of the stack in x; the right ball has a defect (a notch, the part
 * of a sphere with radius 8 at its surface). The analytic volume of the notch
 * is the volume of the lens of the two spheres.
 * <p>
 * The other operations of the Image Calculator for binary masks (0 and 255):
 * AND = intersection, OR = union, XOR = symmetric difference, Subtract = A - B.
 * They measure, e.g., the overlap of two segmentations or channels (Dice,
 * Jaccard) or the change between two time points.
 * <p>
 * For real data: the mirrored side must be aligned (registered) with the defect
 * side first, e.g., with a registration plugin of Fiji. A small misalignment
 * leaves thin slivers along the surface after the subtraction: remove them with
 * a minimum object size (Measure tab) or with a 3D opening of the mask (e.g.,
 * Process > Filters > Minimum 3D, then Maximum 3D).
 */
public class BooleanMasks3d {

	static final int W = 128, H = 64, D = 64;
	static final double R = 20, NOTCH = 8;

	public static void main(String[] args) {
		/* In the background: the iso surface is computed in another thread */
		new Thread(BooleanMasks3d::run, "Bio7 Boolean Masks 3D").start();
	}

	static void run() {
		/* 1. The original stack: a healthy ball (left) and a ball with a defect (right) */
		ImagePlus original = createStack();
		/* 2. The mirrored stack: the healthy ball is now on the defect side */
		ImagePlus mirrored = original.duplicate();
		mirrored.setTitle("Mirrored");
		ImageStack ms = mirrored.getStack();
		for (int z = 1; z <= ms.getSize(); z++) {
			ms.getProcessor(z).flipHorizontal();
		}
		/* 3. The Boolean operations of the Image Calculator (binary masks 0 / 255) */
		ImageCalculator calculator = new ImageCalculator();
		ImagePlus defect = calculator.run("Subtract create stack", mirrored, original);
		defect.setTitle("Defect (Mirrored - Original)");
		ImagePlus and = calculator.run("AND create stack", mirrored, original);
		ImagePlus or = calculator.run("OR create stack", mirrored, original);
		ImagePlus xor = calculator.run("XOR create stack", mirrored, original);
		long a = count(mirrored), b = count(original), n = count(and), u = count(or), x = count(xor);
		long d = count(defect);
		System.out.println("Boolean operations of the masks (voxels)");
		System.out.println("  Mirrored: " + a + ", Original: " + b);
		System.out.println("  Intersection (AND): " + n + ", Union (OR): " + u + ", Symmetric difference (XOR): " + x);
		System.out.println("  Defect (Mirrored - Original): " + d);
		System.out.println(String.format(Locale.US, "  Dice: %.4f, Jaccard: %.4f (the symmetry of the two sides)",
				2.0 * n / (a + b), n / (double) u));

		/* 4. The defect in the 3D view: the original as a volume, the defect as an iso surface */
		Work.openPerspective("com.eco.bio7.perspective_3d");
		sleep(2000);
		IsoSurface iso = SpatialUtil.getIsoSurface();
		Measurement3d measurement = SpatialUtil.getMeasurement();
		VolumeRenderer volume = SpatialUtil.getVolumeRenderer();
		if (iso == null || measurement == null) {
			System.out.println("The 3D view is not available!");
			return;
		}
		if (volume != null) {
			volume.setSelection(ValueSelection.ALL);
			volume.load(original);
		}
		/* The iso surface of the mask (0 / 255) at 127.5: the border between the voxels */
		iso.setSelection(ValueSelection.ALL);
		iso.load(defect);
		iso.setIsoValue(127.5);
		try {
			iso.waitForSurface(60000);
		} catch (InterruptedException e) {
			return;
		}
		iso.setShow(true);
		/* 5. The measurement (also shown in the Bio7 table): the largest object is the defect */
		measurement.setMinObjectSize(10);
		double[][] objects = measurement.measureIsoSurface(true);
		if (objects == null || objects.length == 0) {
			System.out.println("No defect found!");
			return;
		}
		double lens = lensVolume(R, NOTCH, R);
		System.out.println("Volume of the defect");
		System.out.println(String.format(Locale.US, "  Iso surface: %.2f voxels^3, surface area: %.2f voxels^2",
				objects[0][0], objects[0][1]));
		System.out.println(String.format(Locale.US, "  Voxels of the mask: %d, analytic (lens of the spheres): %.2f", d, lens));
		System.out.println("  Objects: " + objects.length + " (thin slivers of a misalignment would be more objects)");
	}

	/* Two balls (radius R) symmetric to the center in x, the right ball with a notch (radius NOTCH) at its surface */
	static ImagePlus createStack() {
		ImageStack stack = new ImageStack(W, H);
		double cy = H / 2.0, cz = D / 2.0;
		double left = W / 2.0 - 30, right = W / 2.0 + 30;
		/* The center of the notch: on the surface of the right ball (in the y direction) */
		double nx = right, ny = cy - R, nz = cz;
		for (int z = 0; z < D; z++) {
			ByteProcessor ip = new ByteProcessor(W, H);
			for (int y = 0; y < H; y++) {
				for (int x = 0; x < W; x++) {
					double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
					boolean inLeft = sq(px - left) + sq(py - cy) + sq(pz - cz) <= R * R;
					boolean inRight = sq(px - right) + sq(py - cy) + sq(pz - cz) <= R * R;
					/* The mirrored notch is not in the left ball: the left ball is the healthy side */
					boolean inNotch = sq(px - nx) + sq(py - ny) + sq(pz - nz) <= NOTCH * NOTCH;
					if (inLeft || (inRight && !inNotch)) {
						ip.set(x, y, 255);
					}
				}
			}
			stack.addSlice("z" + (z + 1), ip);
		}
		ImagePlus imp = new ImagePlus("Original", stack);
		imp.getCalibration().setUnit("voxel");
		return imp;
	}

	/* The volume of the lens of two spheres (radius r1, r2, distance d of the centers) */
	static double lensVolume(double r1, double r2, double d) {
		return Math.PI * sq(r1 + r2 - d) * (d * d + 2 * d * r2 - 3 * r2 * r2 + 2 * d * r1 + 6 * r2 * r1 - 3 * r1 * r1) / (12 * d);
	}

	/* The number of voxels of a mask (value > 0) */
	static long count(ImagePlus imp) {
		long count = 0;
		ImageStack stack = imp.getStack();
		for (int z = 1; z <= stack.getSize(); z++) {
			ImageProcessor ip = stack.getProcessor(z);
			int size = ip.getWidth() * ip.getHeight();
			for (int i = 0; i < size; i++) {
				if (ip.get(i) > 0) {
					count++;
				}
			}
		}
		return count;
	}

	static double sq(double v) {
		return v * v;
	}

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
