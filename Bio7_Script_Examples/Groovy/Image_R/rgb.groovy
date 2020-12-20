import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

/*This example transfers an RGB image as byte vectors to R in one shot!*/

ImagePlus imp = WindowManager.getCurrentImage();

if (imp != null) {
	ImageMethods.imageToR("current", true, 3, null);
}
else{
	Bio7Dialog.message("No image opened in ImageJ!");
}

