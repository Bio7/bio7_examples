/*
Opens several image files (without displaying them), clusters the RGB values of each image
in R (kmeans) and writes the pixel counts of the clusters (the areas) with a scaled image
of each file into a sheet of the Table view (RUtil, the API of the embedded R).

The sheet can be saved and restored with the images and formats as a file.
*/
import ij.IJ
import ij.ImagePlus
import com.eco.bio7.rbridge.RUtil
import static com.eco.bio7.rbridge.Bio7Grid.*
import static com.eco.bio7.batch.Bio7Dialog.*
import static com.eco.bio7.image.ImageMethods.*

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

if (!RUtil.start()) {
	message("R could not be started (see the console)!")
	return
}
/* Several files: select them with Shift+click or Ctrl+click */
String[] files = openMultipleFiles()
if (files == null) {
	return
}
createSheet(0, 0, "Measurements")
for (int i = 0; i < 6; i++) {
	createColumn(1, 70, "Cluster " + (i + 1))
}
createColumn(1, 70, "Name")
createColumn(1, 160, "Image")
for (int i = 0; i < files.length; i++) {
	ImagePlus imp = IJ.openImage(getCurrentPath() + "/" + files[i])
	def buff = new ImagePlus("pic", imp.getProcessor().resize(160, 120)).getBufferedImage()
	rgbToR("currentImage", imp)
	/* The clusters in R and their pixel counts */
	RUtil.exec("cl <- kmeans(currentImage, 6)")
	int[] counts = RUtil.fromRInts("as.integer(table(factor(cl\$cluster, levels = 1:6)))")
	createRow(i, 120)
	for (int u = 0; u < counts.length; u++) {
		setValue(i, u, String.valueOf(counts[u]))
	}
	setValue(i, 6, imp.getTitle())
	setImage(i, 7, toSWTImage(buff))
}
