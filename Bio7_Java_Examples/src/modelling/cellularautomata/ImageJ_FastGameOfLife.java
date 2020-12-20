package modelling.cellularautomata;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Color;

/*
 This is a fast version of the Game of Life in the ImageJ Panel. 
 An image will be automatically created in the ImageJ panel of Bio7  
 if you start the calculation thread!
 This example only works correctly if no image is activated in the ImageJ view!
 */

public class ImageJ_FastGameOfLife extends com.eco.bio7.compile.Model {
	int WIDTH = 1000;
	int HEIGHT = 1000;
	int[][] temp;
	int[] state = { 0, 255 };
	ImagePlus imp;

	/* Constructor */
	public ImageJ_FastGameOfLife() {
		/* If no image is present we create one with random pixels! */
		ImageProcessor ip = new ByteProcessor(WIDTH, HEIGHT);
		ip.setColor(Color.white);
		ip.fill();

		int w = ip.getWidth();
		int h = ip.getHeight();
		temp = new int[w][h];
		for (int u = 0; u < h; u++) {
			for (int v = 0; v < w; v++) {
				int b = (int) (Math.random() * 2);
				int p = ip.getPixel(v, u);
				if (b == 1) {
					ip.putPixel(v, u, state[1]);
				} else {
					ip.putPixel(v, u, state[0]);
				}

			}
		}
		imp = new ImagePlus("Game of Life", ip);
		imp.show();

	}

	public void run() {

		/* The Game of Life routine ! */
		ImageProcessor ip = imp.getProcessor();
		int w = ip.getWidth();
		int h = ip.getHeight();

		for (int i = 0; i < h; i++) {
			for (int u = 0; u < w; u++) {

				int modi = ((i + 1 + h) % (h));// Modulo, no border !
				int modu = ((u + 1 + w) % (w));
				int modni = ((i - 1 + h) % (h));
				int modnu = ((u - 1 + w) % (w));

				int x = ((ip.getPixel(modnu, modni)) + (ip.getPixel(u, modni))
						+ (ip.getPixel(modu, modni)) + (ip.getPixel(modu, i))
						+ (ip.getPixel(modu, modi)) + (ip.getPixel(u, modi))
						+ (ip.getPixel(modnu, modi)) + (ip.getPixel(modnu, i)));

				if (x == 510 // 255=1 + 255=1
						&& ip.getPixel(u, i) == 255
						|| x == 765
						&& ip.getPixel(u, i) == 0
						|| x == 765
						&& ip.getPixel(u, i) == 255) {

					temp[u][i] = state[1];
				} else {
					temp[u][i] = state[0];

				}

			}
		}

		for (int i = 0; i < h; i++) {
			for (int u = 0; u < w; u++) {

				ip.putPixel(u, i, temp[u][i]);

			}
		}

		imp.updateAndDraw();

	}
}