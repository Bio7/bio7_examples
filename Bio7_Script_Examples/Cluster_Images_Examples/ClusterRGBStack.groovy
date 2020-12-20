/*
This example transfers RGB images of a stack to R clusters them (kmeans) and saves them
to the specified directory (please adjust below!).
Please note that values from R will be transferred back to the ImageJ-Canvas then the resulted
image will be saved and closed before the next image of the stack will be transferred to R!

Please load e.g. a sequenze of RGB images as a (virtual) stack to see the results
(Drag a folder of images on the canvas or toolbar)!

*/
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import static com.eco.bio7.rbridge.RServeUtil.*;
import static com.eco.bio7.image.ImageMethods.*;

String dirOut = Bio7Dialog.directory("Choose the output directory!");
if (dirOut != null) {
	if (RServe.isAliveDialog()) {
		int slice = 1;
		double timeOut;
		long time;
		//Calculate the stack size!
		imp = WindowManager.getCurrentImage();
		if (imp != null) {
			int size = imp.getStackSize();
			while (slice <= size) {
				time = System.currentTimeMillis();

				IJ.run("Set Slice...", "slice=" + slice);
				/*Transfer as integer! ->0=double, 1=integer, 2=byte, 3=RGB byte*/
				imageToR("RGBMatrix", true, 3, null);
				evalR("cl<-kmeans(RGBMatrix,6)", null);
				timeOut = (double) (System.currentTimeMillis() - time) / 1000;
				System.out.println("Time: (seconds) " + timeOut + " Fps: " + 1 / timeOut);
				/*In Groovy we have to escape the $ char in strings!*/
				evalR("imageCluster<-as.raw(cl\$cluster)", null);
				imageFromR(3, "imageCluster", 1);
				/*Save the clustered image result as a *.tiff!*/
				IJ.save(IJ.getImage(), dirOut + "/image" + slice + ".tiff");
				IJTabs.deleteActiveTab();
				slice++;
			}
		} else {
			Bio7Dialog.message("No image opened in ImageJ!");
		}
	}
}

