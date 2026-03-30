package modeling.swarm.imagej;

/**
 * Flocking_Plugin
 * 
 * This ImageJ plugin implements a "Boids" flocking simulation. It creates a 
 * dynamic overlay of triangular agents that follow three basic behaviors: 
 * separation (avoiding crowding), alignment (steering towards the average 
 * heading of neighbors), and cohesion (steering toward the center of the group).
 * The simulation runs in real-time on a black canvas until the window is closed 
 * or the ESC key is pressed.
 * 
 * This enhanced version allows for fine-tuning the classic Boids rules:
 * 1. Separation: Avoid hitting neighbors.
 * 2. Alignment: Match velocity/direction with neighbors.
 * 3. Cohesion: Move toward the center of the local group.
 * 4. Jitter: Adds random "noise" to the movement.
 */

import ij.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;
import ij.plugin.PlugIn;

public class Flocking_Plugin implements PlugIn {
	int population = 120;
	double vision = 50.0;
	double minSeparation = 20.0;
	double maxTurn = 0.2;
	double speed = 3.5;

	// New behavioral weights
	double weightSeparation = 1.5;
	double weightAlignment = 1.0;
	double weightCohesion = 0.8;
	double jitter = 0.05; // Randomness factor

	int size = 10;
	int width = 800, height = 600;

	public void run(String arg) {
		IJ.resetEscape();
		if (!showDialog())
			return;

		ImagePlus imp = NewImage.createRGBImage("Flocking Model", width, height, 1, NewImage.FILL_BLACK);
		imp.show();

		ArrayList<Boid> boids = new ArrayList<>();
		for (int i = 0; i < population; i++)
			boids.add(new Boid());

		while (!IJ.escapePressed()) {
			Overlay overlay = new Overlay();

			for (Boid b : boids) {
				b.flock(boids);
				b.update(width, height);

				ShapeRoi sroi = new ShapeRoi(getBoidPolygon(b));
				sroi.setFillColor(Color.CYAN);
				sroi.setStrokeColor(Color.WHITE);
				overlay.add(sroi);
			}

			imp.setOverlay(overlay);
			IJ.wait(30);

			if (imp.getWindow() == null || imp.getWindow().isClosed())
				break;
		}
	}

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Behavioral Settings");
		gd.addMessage("--- Physical Constraints ---");
		gd.addNumericField("Boid Count:", population, 0);
		gd.addNumericField("Vision Range:", vision, 1);
		gd.addNumericField("Max Turn Rate:", maxTurn, 2);
		gd.addNumericField("Speed:", speed, 1);

		gd.addMessage("--- Steer Weights ---");
		gd.addNumericField("Separation Weight:", weightSeparation, 1);
		gd.addNumericField("Alignment Weight:", weightAlignment, 1);
		gd.addNumericField("Cohesion Weight:", weightCohesion, 1);
		gd.addNumericField("Random Jitter:", jitter, 2);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		population = (int) gd.getNextNumber();
		vision = gd.getNextNumber();
		maxTurn = gd.getNextNumber();
		speed = gd.getNextNumber();
		weightSeparation = gd.getNextNumber();
		weightAlignment = gd.getNextNumber();
		weightCohesion = gd.getNextNumber();
		jitter = gd.getNextNumber();
		return true;
	}

	private Polygon getBoidPolygon(Boid b) {
		int x1 = (int) (b.x + Math.cos(b.angle) * size);
		int y1 = (int) (b.y + Math.sin(b.angle) * size);
		int x2 = (int) (b.x + Math.cos(b.angle + 2.6) * size * 0.7);
		int y2 = (int) (b.y + Math.sin(b.angle + 2.6) * size * 0.7);
		int x3 = (int) (b.x + Math.cos(b.angle - 2.6) * size * 0.7);
		int y3 = (int) (b.y + Math.sin(b.angle - 2.6) * size * 0.7);
		return new Polygon(new int[] { x1, x2, x3 }, new int[] { y1, y2, y3 }, 3);
	}

	class Boid {
		double x, y, angle;

		Boid() {
			x = Math.random() * width;
			y = Math.random() * height;
			angle = Math.random() * 2 * Math.PI;
		}

		void flock(ArrayList<Boid> boids) {
			double avgAngle = 0, centerX = 0, centerY = 0, sepX = 0, sepY = 0;
			int count = 0;

			for (Boid other : boids) {
				if (other == this)
					continue;
				double d = Math.hypot(x - other.x, y - other.y);

				if (d < vision) {
					// Alignment data
					avgAngle += other.angle;
					// Cohesion data
					centerX += other.x;
					centerY += other.y;
					// Separation data
					if (d < minSeparation) {
						sepX += (x - other.x);
						sepY += (y - other.y);
					}
					count++;
				}
			}

			if (count > 0) {
				double align = avgAngle / count;
				double cohere = Math.atan2((centerY / count) - y, (centerX / count) - x);
				double separate = (sepX != 0 || sepY != 0) ? Math.atan2(sepY, sepX) : angle;

				// Weighted steering
				double targetAngle = (align * weightAlignment + cohere * weightCohesion + separate * weightSeparation)
						/ (weightAlignment + weightCohesion + (sepX != 0 ? weightSeparation : 0));

				// Add jitter
				targetAngle += (Math.random() - 0.5) * jitter;

				double diff = targetAngle - angle;
				while (diff <= -Math.PI)
					diff += 2 * Math.PI;
				while (diff > Math.PI)
					diff -= 2 * Math.PI;
				angle += Math.max(-maxTurn, Math.min(maxTurn, diff));
			}
		}

		void update(int w, int h) {
			x = (x + Math.cos(angle) * speed + w) % w;
			y = (y + Math.sin(angle) * speed + h) % h;
		}
	}
}
