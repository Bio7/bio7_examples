package modelling.soil;

import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.soil.Matrix;

/*
A simple example to show how to fill data dynamically an 
ImageJ image as a soil layer! 5 layers are created in this example!
The data can be visualized in the 3Dgrid view dependant on the active image
in the ImageJ view!
*/

public class EasyDynamicSoil extends com.eco.bio7.compile.Model {
int WIDTH = Field.getWidth();
int HEIGHT =Field.getHeight();
/*We create a temp array*/
float [][]temp=new float[WIDTH][HEIGHT];
public ImagePlus imp;

public void setup() {
  
    //ImagePlus imp = IJ.createImage("My new image", "32-bit black", 400, 400, 1);  
    //imp.show(); 
	Matrix.create("Nitrate",10000);
	Matrix.create("Phosphate",10000);
	Matrix.create("Carbon",10000);
	Matrix.create("Water",10000);
	Matrix.create("Roots",10000);
	
}
public void run() {
	calculateSoil(1);
	calculateSoil(2);
	calculateSoil(3);
	calculateSoil(4);
	calculateSoil(5);
}

public void calculateSoil(int soil) {
	ImagePlus imp = WindowManager.getImage(soil);
	//ImagePlus imp = WindowManager.getCurrentImage();
	if (imp != null) {
		ImageProcessor ip = imp.getProcessor();
		int w = ip.getWidth();
		int h = ip.getHeight();

		for (int i = 0; i < h; i++) {
			for (int u = 0; u < w; u++) {

				float b = (float) (Math.random() * 10000);
				ip.putPixelValue(u, i, b);
			}
		}
       // ip.resetMinAndMax();Only if the value range changes!
		imp.updateAndDraw();
	}
}
}