package modelling.imagej.cellularautomata;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.*;
import java.util.Random;

/**
 * Interactive Forest Fire Plugin - Ignite via Left-Click (3x3 brush) - Optional
 * Auto-Spread - Optional Random Lightning (Blitzeinschläge) The Rules of Logic
 * In every simulation step (frame), the plugin iterates through every pixel and
 * applies these rules:
 * 
 * Burning Empty: A burning cell always becomes empty in the next step (it
 * burned out). Igniting Burning: An igniting cell (yellow) transitions to full
 * combustion (red). This creates the "moving edge" effect. Growth (Empty Tree):
 * If a cell is empty, it has a small probability to grow a new tree. This
 * allows the forest to recover. Fire Spread (Tree Igniting): A tree catches
 * fire if: Auto-Spread is enabled AND at least one neighbor (8-neighbor Moore
 * neighborhood) is already BURNING or IGNITING, with a probability of
 * spreadProb. OR a random Lightning strike occurs (probability ). OR you click
 * on it with the Mouse.
 */
public class Forest_Fire_ implements PlugIn, MouseListener {

	private static final int EMPTY = 0, TREE = 100, IGNITING = 200, BURNING = 255;
	private ImagePlus imp;
	private ByteProcessor ip;
	private int[][] grid;
	private int w = 600, h = 600;
	private double p, f, spreadProb, initialDensity;
	private boolean autoSpread, lightningEnabled;
	private Random rand = new Random();

	public void run(String arg) {
		IJ.resetEscape();
		GenericDialog gd = new GenericDialog("Forest Fire Master Settings");
		gd.addNumericField("Initial Tree Density (0-1):", 0.65, 2);
		gd.addNumericField("Regrowth Probability (p):", 0.005, 4);

		gd.addMessage("--- Fire Behavior ---");
		gd.addCheckbox("Enable Auto-Spread (Neighbor to Neighbor)", true);
		gd.addNumericField("Spread Probability (0-1):", 0.75, 2);

		gd.addMessage("--- Random Events ---");
		gd.addCheckbox("Enable Random Lightning (Blitzeinschläge)", false);
		gd.addNumericField("Lightning Probability (f):", 0.00005, 6);

		gd.addMessage("Interaction: Left-Click to ignite trees manually!");
		gd.showDialog();

		if (gd.wasCanceled())
			return;

		initialDensity = gd.getNextNumber();
		p = gd.getNextNumber();
		autoSpread = gd.getNextBoolean();
		spreadProb = gd.getNextNumber();
		lightningEnabled = gd.getNextBoolean();
		f = gd.getNextNumber();

		grid = new int[w][h];
		ip = new ByteProcessor(w, h);
		generateForest();

		imp = new ImagePlus("Forest Fire Simulation", ip);
		setupCustomLUT(imp);
		imp.show();
		imp.getCanvas().addMouseListener(this);

		while (imp.getWindow() != null && !IJ.escapePressed()) {
			step();
			imp.updateAndDraw();
			IJ.wait(40);
		}
	}

	private void generateForest() {
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				grid[x][y] = (rand.nextDouble() < initialDensity) ? TREE : EMPTY;
				ip.set(x, y, grid[x][y]);
			}
		}
	}

	private void step() {
		int[][] nextGrid = new int[w][h];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int state = grid[x][y];
				int newState = state;

				if (state == BURNING) {
					newState = EMPTY;
				} else if (state == IGNITING) {
					newState = BURNING;
				} else if (state == TREE) {
					boolean ignited = false;

					// 1. Check for Auto-Spread
					if (autoSpread && isNeighborFiring(x, y) && rand.nextDouble() < spreadProb) {
						ignited = true;
					}

					// 2. Check for Lightning
					if (!ignited && lightningEnabled && rand.nextDouble() < f) {
						ignited = true;
					}

					if (ignited)
						newState = IGNITING;

				} else if (state == EMPTY) {
					if (rand.nextDouble() < p)
						newState = TREE;
				}

				nextGrid[x][y] = newState;
				ip.set(x, y, newState);
			}
		}
		grid = nextGrid;
	}

	private boolean isNeighborFiring(int x, int y) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0)
					continue;
				int nx = (x + dx + w) % w;
				int ny = (y + dy + h) % h;
				if (grid[nx][ny] == BURNING || grid[nx][ny] == IGNITING)
					return true;
			}
		}
		return false;
	}

	private void setupCustomLUT(ImagePlus imp) {
		byte[] r = new byte[256], g = new byte[256], b = new byte[256];
		r[TREE] = 34;
		g[TREE] = (byte) 139;
		b[TREE] = 34;
		r[IGNITING] = (byte) 255;
		g[IGNITING] = (byte) 215;
		b[IGNITING] = 0;
		r[BURNING] = (byte) 220;
		g[BURNING] = 20;
		b[BURNING] = 60;
		imp.getProcessor().setLut(new LUT(r, g, b));
	}

	public void mousePressed(MouseEvent e) {
		int x = imp.getCanvas().offScreenX(e.getX());
		int y = imp.getCanvas().offScreenY(e.getY());
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				int nx = (x + dx + w) % w;
				int ny = (y + dy + h) % h;
				if (grid[nx][ny] == TREE) {
					grid[nx][ny] = IGNITING;
					ip.set(nx, ny, IGNITING);
				}
			}
		}
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}
}
