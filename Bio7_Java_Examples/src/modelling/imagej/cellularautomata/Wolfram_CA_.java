package modelling.imagej.cellularautomata;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;

/**
 * ImageJ Plugin: 1D Cellular Automata (Wolfram Rules)
 * Features: Single rule generation or 256-rule Stack with Autoplay.
 */
public class Wolfram_CA_ implements PlugIn {

    public void run(String arg) {
    	IJ.resetEscape();
        // 1. Setup User Interface
        GenericDialog gd = new GenericDialog("Wolfram 1D CA");
        gd.addNumericField("Single Rule (0-255):", 30, 0);
        gd.addCheckbox("Create Stack of all 256 rules", false);
        gd.addCheckbox("Autoplay Stack (if created)", true);
        gd.addNumericField("Width (pixels):", 501, 0);
        gd.addNumericField("Generations (height):", 250, 0);
        
        gd.showDialog();
        if (gd.wasCanceled()) return;

        int singleRule = (int) gd.getNextNumber();
        boolean createStack = gd.getNextBoolean();
        boolean autoPlay = gd.getNextBoolean();
        int width = (int) gd.getNextNumber();
        int gens = (int) gd.getNextNumber();

        if (createStack) {
            generateStack(width, gens, autoPlay);
        } else {
            generateSingle(singleRule, width, gens);
        }
    }

    /**
     * Generates an ImageStack containing all 256 Wolfram rules.
     */
    private void generateStack(int width, int gens, boolean autoPlay) {
        ImageStack stack = new ImageStack(width, gens);
        
        // Show progress in ImageJ status bar
        for (int r = 0; r < 256; r++) {
            IJ.showStatus("Calculating rule " + r + "/255...");
            IJ.showProgress(r, 255);
            ByteProcessor ip = calculateCA(r, width, gens);
            stack.addSlice("Rule " + r, ip);
        }
        
        ImagePlus imp = new ImagePlus("Wolfram CA Rules 0-255", stack);
        imp.show();

        // Start animation if requested
        if (autoPlay) {
            IJ.run(imp, "Start Animation", "interval=0.1");
        }
    }

    /**
     * Generates a single ImagePlus for one specific rule.
     */
    private void generateSingle(int rule, int width, int gens) {
        ByteProcessor ip = calculateCA(rule, width, gens);
        new ImagePlus("Wolfram Rule " + rule, ip).show();
    }

    /**
     * Core logic to calculate the CA for a specific rule.
     */
    private ByteProcessor calculateCA(int rule, int width, int gens) {
        ByteProcessor ip = new ByteProcessor(width, gens);
        ip.setValue(255); // Background white
        ip.fill();

        int[] currentGen = new int[width];
        currentGen[width / 2] = 1; // Initial state: single pixel
        drawRow(ip, 0, currentGen);

        for (int y = 1; y < gens; y++) {
            int[] nextGen = new int[width];
            for (int x = 0; x < width; x++) {
                int left = currentGen[(x - 1 + width) % width];
                int me = currentGen[x];
                int right = currentGen[(x + 1) % width];
                nextGen[x] = applyRule(left, me, right, rule);
            }
            currentGen = nextGen;
            drawRow(ip, y, currentGen);
        }
        return ip;
    }

    private int applyRule(int a, int b, int c, int rule) {
        int index = (a << 2) | (b << 1) | c;
        return (rule >> index) & 1;
    }

    private void drawRow(ByteProcessor ip, int y, int[] row) {
        for (int x = 0; x < row.length; x++) {
            int val = (row[x] == 1) ? 0 : 255;
            ip.set(x, y, val);
        }
    }
}
