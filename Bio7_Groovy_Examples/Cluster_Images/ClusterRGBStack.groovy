/*
Clusters the RGB images of a stack in R (kmeans) and saves the clustered images
in a directory (RUtil, the API of the embedded R).

Load, e.g., a sequence of RGB images as a (virtual) stack before
(drag a folder of images on the ImageJ canvas or toolbar).
*/
import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.process.ByteProcessor
import com.eco.bio7.rbridge.RUtil

/* The R, G and B values of an image as a matrix in R (one row per pixel, the columns r, g and b) */
def rgbToR(String name, ImagePlus imp) {
	int[] px = (int[]) imp.getProcessor().convertToRGB().getPixels()
	int[] r = new int[px.length], g = new int[px.length], b = new int[px.length]
	for (int i = 0; i < px.length; i++) {
		r[i] = (px[i] >> 16) & 0xff; g[i] = (px[i] >> 8) & 0xff; b[i] = px[i] & 0xff
	}
	RUtil.dataFrameToR(".rgb", [r: r, g: g, b: b])
	RUtil.exec(name + " <- as.matrix(.rgb); rm(.rgb)")
}

if (!RUtil.isAlive()) {
	Bio7Dialog.message("R is not running! Please start R first.")
	return
}
ImagePlus imp = WindowManager.getCurrentImage()
if (imp == null) {
	Bio7Dialog.message("No image opened in ImageJ!")
	return
}
String dirOut = Bio7Dialog.directory("Choose the output directory!")
if (dirOut == null) {
	return
}
int w = imp.getWidth(), h = imp.getHeight()
for (int slice = 1; slice <= imp.getStackSize(); slice++) {
	long time = System.currentTimeMillis()
	rgbToR("RGBMatrix", new ImagePlus("slice", imp.getStack().getProcessor(slice)))
	RUtil.exec("cl <- kmeans(RGBMatrix, 6)")
	/* The clusters (1-6) as an 8-bit image: the pixels in the order of the rows */
	int[] cluster = RUtil.fromRInts("cl\$cluster")
	ByteProcessor bp = new ByteProcessor(w, h)
	for (int i = 0; i < cluster.length; i++) {
		bp.set(i, cluster[i] * 40)
	}
	IJ.save(new ImagePlus("cluster" + slice, bp), dirOut + "/image" + slice + ".tif")
	println("Slice " + slice + ": " + (System.currentTimeMillis() - time) / 1000.0 + " s")
}
