package modelling.cellularautomata;

import ij.*;
import ij.process.*;
import ij.plugin.PlugIn;
import java.util.stream.IntStream;

/**
 * Fast Parallel Game of Life for ImageJ (ByteProcessor version)
 * - Replaces ShortProcessor/short[] with ByteProcessor/byte[] (0 or 255 values)
 * - One-loop double-buffering approach and parallel per-row processing
 */
public class ImageJ_FastGameOfLife_Parallel_One_Loop_Byte_ implements PlugIn {
    int WIDTH = 5000, HEIGHT = 5000, n = 1000;

    public void run(String arg) {
    	IJ.resetEscape();
        // Initialize the ImageJ Processor and the first buffer (8-bit)
        ByteProcessor ip = new ByteProcessor(WIDTH, HEIGHT);
        byte[] pixelsA = (byte[]) ip.getPixels();
        
        // Initialize the second buffer (back-buffer)
        byte[] pixelsB = new byte[WIDTH * HEIGHT];
        
        // Randomize initial state in pixelsA (0 or 255)
        for (int i = 0; i < pixelsA.length; i++) {
            pixelsA[i] = (byte)(Math.random() > 0.5 ? 255 : 0);
        }

        ImagePlus imp = new ImagePlus("Turbo Game of Life (Byte)", ip);
        imp.setDisplayRange(0, 255);
        imp.show();

        // Pre-calculate neighbor offsets for periodic boundaries (Toroidal)
        int[] x_m = new int[WIDTH], x_p = new int[WIDTH];
        int[] y_m = new int[HEIGHT], y_p = new int[HEIGHT];
        for (int i = 0; i < WIDTH; i++) { 
            x_m[i] = (i - 1 + WIDTH) % WIDTH; 
            x_p[i] = (i + 1) % WIDTH; 
        }
        for (int i = 0; i < HEIGHT; i++) { 
            y_m[i] = ((i - 1 + HEIGHT) % HEIGHT) * WIDTH; 
            y_p[i] = ((i + 1) % HEIGHT) * WIDTH; 
        }

        // Put buffers in an array for easy toggling
        byte[][] buffers = {pixelsA, pixelsB};

        for (int it = 0; it < n; it++) {
            if (IJ.escapePressed()) break;

            // Identify which buffer is "Current" and which is "Next"
            final byte[] currentGen = buffers[it % 2];
            final byte[] nextGen = buffers[(it + 1) % 2];

            // Parallel Processing: Calculate nextGen based on currentGen
            IntStream.range(0, HEIGHT).parallel().forEach(y -> {
                int y_offset = y * WIDTH;
                int ym = y_m[y];
                int yp = y_p[y];
                
                for (int x = 0; x < WIDTH; x++) {
                    // Sum neighbors (mask with 0xff to interpret bytes as unsigned)
                    int neighbors = (currentGen[x_m[x] + ym] & 0xff) + (currentGen[x + ym] & 0xff) + (currentGen[x_p[x] + ym] & 0xff)
                                  + (currentGen[x_m[x] + y_offset] & 0xff)                         + (currentGen[x_p[x] + y_offset] & 0xff)
                                  + (currentGen[x_m[x] + yp] & 0xff) + (currentGen[x + yp] & 0xff) + (currentGen[x_p[x] + yp] & 0xff);

                    int currentState = currentGen[x + y_offset] & 0xff;
                    
                    // Game of Life Logic:
                    // 3 neighbors * 255 = 765 (Birth)
                    // 2 neighbors * 255 = 510 (Survival)
                    if (neighbors == 765 || (neighbors == 510 && currentState == 255)) {
                        nextGen[x + y_offset] = (byte) 255;
                    } else {
                        nextGen[x + y_offset] = (byte) 0;
                    }
                }
            });

            // Update the ImageProcessor to point to the newly calculated generation
            ip.setPixels(nextGen);
            
            imp.updateAndDraw();
            IJ.showStatus("Iteration: " + (it + 1) + " (Press Esc to stop)");
        }
    }
}
