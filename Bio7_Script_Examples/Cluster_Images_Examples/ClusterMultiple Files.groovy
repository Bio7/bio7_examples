/*
This example opens several image files (if selected) in ImageJ without to displaying them in the ImageJ-Canvas view. 
The script transfers, clusters and summarizes the RGB pixel values of the images in R
Then it transfers the summarized results (pixel counts - areas) to the Bio7 Grid. The default images are scaled
and are painted in a grid cell, too!

The new Bio7 API method to transfer R, G, B values as a matrix is used for this workflow! 

Note: This example demonstrates how to apply advanced RScripts to e.g. a folder of images and then
generate automatically a report of the results in the Bio7 spreadsheet (which can be saved and 
restored as a file with the new Bio7 import/export format which saves images and formattings of
the sheet, too).
*/
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import org.eclipse.nebula.widgets.grid.Grid;
import ij.process.ImageProcessor;
import java.awt.image.BufferedImage;
import static com.eco.bio7.rbridge.RServeUtil.*;
import static com.eco.bio7.rbridge.Bio7Grid.*;
import static com.eco.bio7.batch.Bio7Dialog.*;
import static com.eco.bio7.image.ImageMethods.*;

if (RServe.isAliveDialog()) {
createSheet(0, 0, "Measurements");//Create a new sheet in the Table view!
for (int i = 0; i < 6; i++) {
	createColumn(1,70, "State: " + (i + 1));//Create new columns!
	
}
createColumn(1,70, "Name");//Create new column!
createColumn(1,160, "Image");//Create new column!

/*
Open files from multiple selections(with Shift+Mouse-Click or Ctrl+Mouse-Click).
*/
String[] files = openMultipleFiles();
if(files!=null){	
	for (int i = 0; i < files.length; i++) {
		println(getCurrentPath() + "/" + files[i]);
		imp = IJ.openImage(getCurrentPath() + "/" + files[i]);//Open  image data with the ImageJ API!
		ip = imp.getProcessor().resize(160, 120);//Resize the image data!
		impResized=new ImagePlus("pic", ip);//Create a new image from the image data!
		buff=impResized.getBufferedImage();//Create a BufferedImage from the resized image!
	
	    //Transfer the R, G, B image (ImagePlus required as argument!) to R in one shot with the new Bio7 1.6 API method!
		imageToR("currentImage", true, 3, imp); 
		evalR("cl<-kmeans(currentImage,6)",null); //Cluster the image data with the kmeans algorithm in R!
		/*In Groovy we have to escape the $ char in strings!*/
		evalR("print(table(cl\$cluster))",null);//Create a table in R from the results!
		String[] values = (String[])fromR("as.character(table(cl\$cluster))").asStrings();//Get the result values as a String array!
		createRow(i,120);
		/*Put the values and the scaled image in the sheet!*/
			for (int u = 0; u < values.length; u++) {
				setValue(i, u, values[u]);
				setValue(i, 6, imp.getTitle());
				setImage(i,7,toSWTImage(buff));
			}

		}
	}
}
