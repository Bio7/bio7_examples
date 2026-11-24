/*
This snippet demonstrates the Game of Life with Groovy and ImageJ.
A new image is created and updated 100 times in a loop by means of Groovy.
Please change to the Image perspective to see the results!
*/
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;
import ij.WindowManager;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
int WIDTH = 200;
int HEIGHT =200;
int [][]temp;
state=[0,255];
int count=0;

	

while(count<100) {
    
    ImagePlus imp = WindowManager.getCurrentImage();

    if (imp == null) {
        /*If no image is present we create one with random pixels!*/
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
    } else {
        /*The Game of Life routine !*/
        ImageProcessor ip = imp.getProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();

        for (int i = 0; i < h; i++) {
            for (int u = 0; u < w; u++) {

                int modi = ((i + 1 + h) % (h));//Modulo, no border !
                int modu = ((u + 1 + w) % (w));
                int modni = ((i - 1 + h) % (h));
                int modnu = ((u - 1 + w) % (w));

                

                int x = ((ip.getPixel(modnu, modni))
                    + (ip.getPixel(u, modni))
                    + (ip.getPixel(modu,modni ))
                    + (ip.getPixel(modu,i))
                    + (ip.getPixel(modu,modi ))
                    + (ip.getPixel(u,modi))
                    + (ip.getPixel(modnu,modi )) + (ip.getPixel(modnu,i)));
                

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
count++;
}
