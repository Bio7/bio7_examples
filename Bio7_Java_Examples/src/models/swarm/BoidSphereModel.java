/*
* The example demonstrates the use of a custom camera and tracks the motion
* of a glider in a swarm model (each boid a glider of triangles: it points in its
* direction of flight, climbs, dives and banks into turns)! Please invoke the setup method in the "Options Space" view for a correct display.
* Then Invoke the Play/Pause action to draw the OpenGL commands.
* Enable the "Custom Camera" view option in the "Camera" tab (optional the "Split View" option) 
* to see the result of a custom camera!
* 
* This example comes with a "Settings" view to adjust some Boid parameters.
*
* The flock (as the WebGL example web/BoidsGlGui): each boid steers in 3D by the
* boids within its neighbourhood - separation (away from close boids), alignment
* (the mean direction) and cohesion (to their centre); it turns back softly at the
* walls of the flight space (above the floor) and flies between half and the full
* maximum speed. All boids decide first, then all move (one time step).
* 
* Stop the "Play/Pause" action to recompile and press "Setup" again.
*/
package models.swarm;

import java.util.ArrayList;
import java.util.Date;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.SpatialCamera;
import com.eco.bio7.spatial.SpatialStructure;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;

import static com.jogamp.opengl.GL2.*;
import static com.eco.bio7.spatial.SpatialEvents.*;

public class BoidSphereModel extends com.eco.bio7.compile.Model {

	private ArrayList<Boid> boids;
	public DoubleMersenneTwister twist = new DoubleMersenneTwister(new Date());

	// GUI Parameters (initial values)
	public double maxSpeed = 6.2;
	public double maxForce = 0.05;
	public double separationWeight = 1.5;
	public double alignmentWeight = 1.0;
	public double cohesionWeight = 1.0;
	public double camDist = 20.0;
	/* The number of boids (changed live: added or removed at the next frame) */
	public volatile int boidCount = 200;
	/* The angle of the global view (Settings: "Global view") */
	public static final String[] GLOBAL_VIEWS = { "Oblique (45°)", "Top down (90°)", "Low (15°)" };
	public volatile String globalView = GLOBAL_VIEWS[0];
	public float radius = 3.5f;
	/* Obstacles: pillars which the boids fly around (Settings: on/off, number, placement) */
	public static final String[] PLACEMENTS = { "Random", "Fixed (centre)" };
	public volatile boolean obstaclesEnabled = false;
	public volatile int obstacleCount = 8;
	public volatile String obstaclePlacement = PLACEMENTS[0];
	/* The pillars: x, y, radius (made again in the thread of the drawing if requested) */
	private volatile double[][] obstacles = new double[0][];
	private volatile boolean newObstacles = true;

	/* The size of the floor (in 100 units): it also sets the flight space and the box of the view */
	public volatile int FLOOR_LEN = 20;
	public static float[] TransformMatrix = new float[16];

	/* Start after compilation if run action is active! */
	public BoidSphereModel() {
		
		setup();
	}

	public void setup() {
		setupModel();
	}

	private void setupModel() {		
		Work.closePerspective("3d");
		Work.openPerspective("com.eco.bio7.CustomPerspective");
		Work.openView("com.eco.bio7.spatial");
		// Work.openPerspective("com.eco.bio7.perspective_3d");
		// Initialize the custom SWT GUI
		createGui();

		// The angle of the global view (oblique, top down or low)
		applyGlobalView();
		// The box, the grids and the axes: the floor and the flight space
		updateSpace();
		SpatialUtil.setSplitView(true);
		SpatialUtil.setCustomCamera(true);
		/*
		 * The custom camera in a fixed frame (as the walkthrough: without the angle of
		 * the global view and the mouse rotation): it follows the boid with every
		 * global view
		 */
		SpatialCamera.setCustomCameraWalkthroughCoordinates(true);
		// Initialize Boid population (in the flight space above the floor)
		boids = new ArrayList<Boid>();
		for (int i = 0; i < boidCount; i++) {
			boids.add(randomBoid());
		}
		/* Start the run method! */
		if (!SpatialUtil.isStarted())
			SpatialUtil.startStop();

	}

	/* A boid at a random place of the flight space */
	private Boid randomBoid() {
		double half = flightHalf(), low = Flock.FLOOR_Z, top = flightTop();
		return new Boid((twist.nextDouble() - 0.5) * 1.3 * half, (twist.nextDouble() - 0.5) * 1.3 * half, low + (0.2 + 0.6 * twist.nextDouble()) * (top - low), maxSpeed);
	}

	/* The half width of the floor (it is centred at the origin, at z = 0) */
	private double floorHalf() {
		return FLOOR_LEN * 50.0;
	}

	/* The flight space: x and y within +- this value (90 % of the floor) */
	private double flightHalf() {
		return floorHalf() * 0.9;
	}

	/* The flight space: the top (the height grows with the floor) */
	private double flightTop() {
		return Flock.FLOOR_Z + Math.max(300, FLOOR_LEN * 30.0);
	}

	/** The obstacles are made again at the next frame (e.g., another number or placement, new random positions). */
	public void requestObstacles() {
		newObstacles = true;
	}

	/*
	 * The pillars: random (spread over the flight space, a little apart, different
	 * thickness) or fixed (one in the centre, then rings around it)
	 */
	private double[][] makeObstacles() {
		int n = obstaclesEnabled ? Math.max(0, obstacleCount) : 0;
		double[][] o = new double[n][];
		double half = flightHalf();
		boolean random = obstaclePlacement.startsWith("Random");
		for (int i = 0; i < n; i++) {
			if (random) {
				double r = 30 + twist.nextDouble() * 50;
				double x = 0, y = 0;
				/* A place a little apart from the others (some tries) */
				for (int t = 0; t < 30; t++) {
					x = (twist.nextDouble() * 2 - 1) * half * 0.8;
					y = (twist.nextDouble() * 2 - 1) * half * 0.8;
					boolean free = true;
					for (int k = 0; k < i; k++) {
						if (Math.hypot(x - o[k][0], y - o[k][1]) < r + o[k][2] + 120) {
							free = false;
							break;
						}
					}
					if (free) {
						break;
					}
				}
				o[i] = new double[] { x, y, r };
			} else {
				/* Fixed: the centre, then rings of 6, 12, 18 ... pillars */
				double r = 45;
				if (i == 0) {
					o[i] = new double[] { 0, 0, r };
				} else {
					int ring = 1, first = 1;
					while (i >= first + 6 * ring) {
						first += 6 * ring;
						ring++;
					}
					int inRing = 6 * ring, k = i - first;
					/* The rings within the flight space (a smaller floor: closer rings) */
					int rings = 0;
					for (int m = 1; m < n; m += 6 * (rings + 1)) {
						rings++;
					}
					double spacing = Math.min(220, half * 0.85 / Math.max(1, rings));
					double dist = ring * spacing, angle = 2 * Math.PI * k / inRing + ring * 0.3;
					o[i] = new double[] { dist * Math.cos(angle), dist * Math.sin(angle), r };
				}
			}
		}
		return o;
	}

	/**
	 * Sets the size of the floor (in 100 units): the flight space follows, the box,
	 * the grids and the axes of the view are fitted to the floor and the flight
	 * space.
	 */
	public void setFloorSize(int size) {
		FLOOR_LEN = Math.max(1, size);
		updateSpace();
		/* The obstacles fitted to the new flight space */
		requestObstacles();
	}

	/* The box, the grids and the axes of the 3D view: the floor and the flight space */
	private void updateSpace() {
		double h = floorHalf();
		SpatialUtil.setDataBounds(-h, h, -h, h, 0, flightTop());
	}

	/**
	 * Sets the angle of the global view (a mouse rotation is reset): oblique
	 * (45° down), top down (90°) or low (15°).
	 */
	public void applyGlobalView() {
		float angle = globalView.startsWith("Top") ? -90f : globalView.startsWith("Low") ? -15f : -45f;
		SpatialUtil.resetRotation();
		SpatialUtil.setRotationX(angle);
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setupModel();

	}

	private void createGui() {

		CustomView view = new CustomView();
		Display d = Display.getDefault();
		d.syncExec(() -> {
			// Create a tab in Bio7's Custom View

			CustomView customView = new CustomView();
			// Move default Options3D view widgets to a custom view with a predefined
			// location of the Custom perspective.
			moveViewToPanel(customView, "com.eco.bio7.discrete3d.Options3d", "subLeftTop", "Options3D");
			// Create a custom view with a predefined location of the Custom perspective.
			Composite parent = view.getComposite("subLeftBottom");
			/* Also rename view else id is used! */
			renamePanelTab(parent, "Settings");
			parent.setLayout(new FillLayout());
			// Instantiate our manual GUI class
			new Settings(parent, BoidSphereModel.this);
			parent.layout(true);
		});
	}

	/*
	 * One time step of the flock and the camera: once per frame, before the cameras
	 * of the frame are set (SpatialStructure.beforeFrame). The views of the frame
	 * (both views of the split view) show the same step, the camera behind the
	 * boid belongs to this step (set in run(), the camera would apply to the next
	 * view: the boid would shake in the camera).
	 */
	private final Runnable frameStart = this::frameStart;

	private void frameStart() {
		SpatialStructure structure = SpatialStructure.getSpatialStructureInstance();
		if (com.eco.bio7.methods.Compiled.getModel() != this || structure == null || !structure.isFromOpenGl()) {
			return;
		}
		/* The number of boids of the settings (the first boid, the one of the camera, stays) */
		while (boids.size() < boidCount) {
			boids.add(randomBoid());
		}
		while (boids.size() > Math.max(1, boidCount)) {
			boids.remove(boids.size() - 1);
		}
		/* The obstacles of the settings (made again if requested) */
		if (newObstacles) {
			newObstacles = false;
			obstacles = makeObstacles();
		}
		/* One time step of the flock (all boids decide, then all move) */
		Flock.step(boids, maxSpeed, maxForce, separationWeight, alignmentWeight, cohesionWeight, flightHalf(), flightTop(), obstacles);

		// Camera tracking logic for the lead boid
		if (!boids.isEmpty()) {
			Boid target = boids.get(0);
			/* Behind the glider in its smoothed direction (the camera does not jitter) */
			double speed = Math.max(1e-9, Math.hypot(target.dirX, target.dirY));
			double dx = target.dirX / speed;
			double dy = target.dirY / speed;

			// Set camera using dynamic distance from GUI
			SpatialCamera.setCustomCamera(target.x - (dx * camDist), target.z + 10.0, target.y - (dy * camDist),
					target.x, target.z, target.y);
		}
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		/* The step and the camera before each frame (frameStart) */
		SpatialStructure.beforeFrame = frameStart;
		gl.glEnable(GL_COLOR_MATERIAL);
		/* Both sides of the wings lit (a glider is open, not a closed body) */
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
		gl.glEnable(GL2.GL_NORMALIZE);
		/* The floor with the shadows of the boids (below them), the obstacles */
		drawFloor(gl);
		drawObstacles(gl, glu);
		/* The gliders (drawn in each view of the frame: the same step) */
		for (int i = 0; i < boids.size(); i++) {
			/* The boid of the camera (the first one) is red: visible in the global view */
			boids.get(i).render(gl, radius, i == 0 ? RED : null);
		}
		/* Add a boid if the mouse is double-clicked! */
		if (isMouseDoubleClicked()) {
			boids.add(new Boid(0, 0, (Flock.FLOOR_Z + flightTop()) / 2, maxSpeed));
			boidCount = boids.size();
		}
	}

	/*
	 * The glider of a boid (nose along +x, wings in the x-y plane, the fin up +z),
	 * in units of the size ("Size" in the settings): a dart with a ridge on the
	 * top and the bottom, a notch at the tail and a fin.
	 */
	/* The color of the boid which the camera follows */
	private static final float[] RED = { 0.95f, 0.15f, 0.12f };

	private static final double[] NOSE = { 2.2, 0, 0 }, LEFT = { -1.3, 1.7, 0 }, RIGHT = { -1.3, -1.7, 0 }, TAIL = { -0.7, 0, 0 },
			TOP = { -0.2, 0, 0.28 }, BOTTOM = { -0.2, 0, -0.22 }, FIN = { -1.25, 0, 0.95 };

	/**
	 * The rules of the flock (without OpenGL): one time step of all boids. The
	 * weights are the ones of the settings (defaults: separation 1.5, alignment 1,
	 * cohesion 1).
	 */
	static final class Flock {

		/* The neighbourhood, the distance of "close" boids */
		static final double RADIUS = 150, CLOSE = 45;
		/* The lowest height of the flight (the floor is at z = 0) */
		static final double FLOOR_Z = 120;

		private Flock() {
		}

		/*
		 * One time step; the flight space: x and y within +-half, z between FLOOR_Z and
		 * top (the boids turn back softly near its walls)
		 */
		static void step(java.util.List<Boid> boids, double maxSpeed, double maxForce, double separationWeight, double alignmentWeight,
				double cohesionWeight, double half, double top) {
			step(boids, maxSpeed, maxForce, separationWeight, alignmentWeight, cohesionWeight, half, top, new double[0][]);
		}

		/* The distance at which a boid starts to avoid a pillar (beyond its surface) */
		static final double AVOID = 110;

		/*
		 * As step(...); obstacles: vertical pillars (x, y, radius) from the floor to
		 * the top, the boids fly around them
		 */
		static void step(java.util.List<Boid> boids, double maxSpeed, double maxForce, double separationWeight, double alignmentWeight,
				double cohesionWeight, double half, double top, double[][] obstacles) {
			final double HALF = half, TOP_Z = top, MARGIN = Math.min(120, half * 0.2);
			int n = boids.size();
			double[] nv = new double[3 * n];
			/* The largest change of the velocity in one step (Max Force: how sharp a boid turns) */
			double maxTurn = maxForce * 12;
			double wallTurn = 0.1 * maxSpeed;
			for (int i = 0; i < n; i++) {
				Boid b = boids.get(i);
				int cnt = 0;
				double ax = 0, ay = 0, az = 0, cx = 0, cy = 0, cz = 0, px = 0, py = 0, pz = 0;
				for (int j = 0; j < n; j++) {
					if (j == i) {
						continue;
					}
					Boid o = boids.get(j);
					double dx = o.x - b.x, dy = o.y - b.y, dz = o.z - b.z;
					double d2 = dx * dx + dy * dy + dz * dz;
					if (d2 > RADIUS * RADIUS || d2 == 0) {
						continue;
					}
					cnt++;
					ax += o.vx;
					ay += o.vy;
					az += o.vz;
					cx += dx;
					cy += dy;
					cz += dz;
					if (d2 < CLOSE * CLOSE) {
						/* Separation: the closer, the stronger (1 / distance) */
						px -= dx / d2;
						py -= dy / d2;
						pz -= dz / d2;
					}
				}
				double sx = 0, sy = 0, sz = 0;
				if (cnt > 0) {
					sx = alignmentWeight * 0.06 * (ax / cnt - b.vx) + cohesionWeight * 0.0004 * cx / cnt + separationWeight * 2.5 * px;
					sy = alignmentWeight * 0.06 * (ay / cnt - b.vy) + cohesionWeight * 0.0004 * cy / cnt + separationWeight * 2.5 * py;
					sz = alignmentWeight * 0.06 * (az / cnt - b.vz) + cohesionWeight * 0.0004 * cz / cnt + separationWeight * 2.5 * pz;
				}
				/* The steering limited by Max Force */
				double s = Math.sqrt(sx * sx + sy * sy + sz * sz);
				if (s > maxTurn) {
					sx *= maxTurn / s;
					sy *= maxTurn / s;
					sz *= maxTurn / s;
				}
				/*
				 * The pillars: a boid looks ahead (its position in 15 steps); the closer its
				 * path to a pillar, the stronger it steers away (sideways, not limited by Max
				 * Force: an obstacle has priority)
				 */
				for (double[] o : obstacles) {
					double fx = b.x + b.vx * 15 - o[0], fy = b.y + b.vy * 15 - o[1];
					double nx = b.x - o[0], ny = b.y - o[1];
					double d = Math.min(Math.hypot(fx, fy), Math.hypot(nx, ny)) - o[2];
					if (d < AVOID) {
						double len = Math.max(1e-9, Math.hypot(fx, fy));
						double push = maxSpeed * 0.25 * (1 - Math.max(0, d) / AVOID);
						sx += fx / len * push;
						sy += fy / len * push;
					}
				}
				/* The walls: turn back softly near the edges of the flight space */
				if (b.x > HALF - MARGIN) sx -= wallTurn;
				if (b.x < -HALF + MARGIN) sx += wallTurn;
				if (b.y > HALF - MARGIN) sy -= wallTurn;
				if (b.y < -HALF + MARGIN) sy += wallTurn;
				if (b.z > TOP_Z - MARGIN) sz -= wallTurn;
				if (b.z < FLOOR_Z + MARGIN) sz += wallTurn;
				double vx = b.vx + sx, vy = b.vy + sy, vz = b.vz + sz;
				/* The speed between a half and the whole maximum */
				double v = Math.sqrt(vx * vx + vy * vy + vz * vz);
				double f = v > maxSpeed ? maxSpeed / v : v < maxSpeed / 2 ? maxSpeed / 2 / Math.max(v, 1e-9) : 1;
				nv[3 * i] = vx * f;
				nv[3 * i + 1] = vy * f;
				nv[3 * i + 2] = vz * f;
			}
			/* All move */
			for (int i = 0; i < n; i++) {
				Boid b = boids.get(i);
				b.vx = nv[3 * i];
				b.vy = nv[3 * i + 1];
				b.vz = nv[3 * i + 2];
				/* Never inside a pillar: pushed out to its surface */
				for (double[] o : obstacles) {
					double nx = b.x - o[0], ny = b.y - o[1], d = Math.hypot(nx, ny);
					if (d < o[2] + 2) {
						double len = Math.max(1e-9, d);
						b.x = o[0] + nx / len * (o[2] + 2);
						b.y = o[1] + ny / len * (o[2] + 2);
					}
				}
				/* Not far outside (e.g., after a smaller floor: the boids come back) */
				double out = Math.min(200, HALF * 0.1);
				b.x = Math.max(-HALF - out, Math.min(HALF + out, b.x + b.vx));
				b.y = Math.max(-HALF - out, Math.min(HALF + out, b.y + b.vy));
				b.z = Math.max(FLOOR_Z * 0.5, Math.min(TOP_Z + out, b.z + b.vz));
				b.smoothDirection();
			}
		}

		/** The polarization: the length of the mean flight direction (0: disordered, 1: all fly in parallel). */
		static double polarization(java.util.List<Boid> boids) {
			double sx = 0, sy = 0, sz = 0;
			for (Boid b : boids) {
				double v = Math.max(1e-9, Math.sqrt(b.vx * b.vx + b.vy * b.vy + b.vz * b.vz));
				sx += b.vx / v;
				sy += b.vy / v;
				sz += b.vz / v;
			}
			return Math.sqrt(sx * sx + sy * sy + sz * sz) / Math.max(1, boids.size());
		}
	}

	static final class Boid {
		double x, y, z, vx, vy, vz;
		/* The heading of the last step and the bank (the roll into a turn, degrees) */
		double lastYaw = Double.NaN, bank;
		/* The direction of the glider (a unit vector): the velocity smoothed over the steps */
		double dirX = 1, dirY, dirZ;
		/* Each boid a shade of blue-cyan */
		final float[] color;

		/* A boid at a position, flying in a random direction (three quarters of the maximum speed) */
		Boid(double x, double y, double z, double maxSpeed) {
			this.x = x;
			this.y = y;
			this.z = z;
			java.awt.Color c = java.awt.Color.getHSBColor((float) (0.50 + Math.random() * 0.12), 0.75f, 0.95f);
			color = c.getRGBColorComponents(null);
			double angle = Math.random() * Math.PI * 2;
			double climb = (Math.random() - 0.5) * 0.6;
			double speed = 0.75 * maxSpeed;
			this.vx = Math.cos(angle) * Math.cos(climb) * speed;
			this.vy = Math.sin(angle) * Math.cos(climb) * speed;
			this.vz = Math.sin(climb) * speed;
			double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
			dirX = vx / len;
			dirY = vy / len;
			dirZ = vz / len;
		}

		/*
		 * The direction smoothed over the steps (half the old direction, half the new
		 * velocity): the gliders do not jitter (climb, dive, turn) with the small
		 * changes of the velocity in each step
		 */
		void smoothDirection() {
			double v = Math.sqrt(vx * vx + vy * vy + vz * vz);
			if (v <= 0) {
				return;
			}
			double nx = 0.5 * dirX + 0.5 * vx / v, ny = 0.5 * dirY + 0.5 * vy / v, nz = 0.5 * dirZ + 0.5 * vz / v;
			double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (len < 1e-3) {
				/* A reversal (opposite directions): the new direction */
				nx = vx;
				ny = vy;
				nz = vz;
				len = v;
			}
			dirX = nx / len;
			dirY = ny / len;
			dirZ = nz / len;
			/* Banking: the glider rolls into a turn (smoothed; y inverted as the position) */
			double yaw = Math.toDegrees(Math.atan2(-dirY, dirX));
			if (!Double.isNaN(lastYaw)) {
				double turn = yaw - lastYaw;
				if (turn > 180) {
					turn -= 360;
				} else if (turn < -180) {
					turn += 360;
				}
				bank = 0.85 * bank + 0.15 * Math.max(-50, Math.min(50, -turn * 12));
			}
			lastYaw = yaw;
		}

		/* Draws the glider; color: its color, null: the color of the boid */
		void render(GL2 gl, float radius, float[] highlight) {
			float[] color = highlight != null ? highlight : this.color;
			/* The direction of the flight, smoothed over the steps (y inverted as the position) */
			double dx = dirX, dy = -dirY, dz = dirZ;
			double yaw = Math.toDegrees(Math.atan2(dy, dx));
			double pitch = Math.toDegrees(Math.atan2(dz, Math.hypot(dx, dy)));
			/* The bank of the step (smoothDirection) */

			gl.glPushMatrix();
			gl.glTranslated(x, -y, z); // Bio7 Y-Inversion
			gl.glRotated(yaw, 0, 0, 1);
			gl.glRotated(-pitch, 0, 1, 0);
			gl.glRotated(bank, 1, 0, 0);
			gl.glScaled(radius, radius, radius);
			gl.glBegin(GL2.GL_TRIANGLES);
			/* The top (the color of the boid) */
			gl.glColor3f(color[0], color[1], color[2]);
			triangle(gl, NOSE, LEFT, TOP);
			triangle(gl, NOSE, TOP, RIGHT);
			triangle(gl, LEFT, TAIL, TOP);
			triangle(gl, TOP, TAIL, RIGHT);
			/* The bottom (darker) */
			gl.glColor3f(color[0] * 0.45f, color[1] * 0.45f, color[2] * 0.55f);
			triangle(gl, NOSE, BOTTOM, LEFT);
			triangle(gl, NOSE, RIGHT, BOTTOM);
			triangle(gl, LEFT, BOTTOM, TAIL);
			triangle(gl, BOTTOM, RIGHT, TAIL);
			/* The fin (light) */
			gl.glColor3f(0.85f + color[0] * 0.15f, 0.9f, 1.0f);
			triangle(gl, TOP, TAIL, FIN);
			gl.glEnd();
			gl.glPopMatrix();
		}

		/* A triangle with its normal (for the lighting) */
		private static void triangle(GL2 gl, double[] a, double[] b, double[] c) {
			double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
			double wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
			gl.glNormal3d(uy * wz - uz * wy, uz * wx - ux * wz, ux * wy - uy * wx);
			gl.glVertex3d(a[0], a[1], a[2]);
			gl.glVertex3d(b[0], b[1], b[2]);
			gl.glVertex3d(c[0], c[1], c[2]);
		}
	}

	/* The pillars (lit, from the floor to near the top of the flight space; y inverted as the boids) */
	private com.jogamp.opengl.glu.GLUquadric quadric;

	private void drawObstacles(GL2 gl, GLU glu) {
		double[][] o = obstacles;
		if (o.length == 0) {
			return;
		}
		if (quadric == null) {
			quadric = glu.gluNewQuadric();
			glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);
		}
		double height = flightTop() * 0.95;
		for (double[] p : o) {
			gl.glPushMatrix();
			gl.glTranslated(p[0], -p[1], 0);
			/* The pillar (a light, slightly blue stone), the top a little lighter */
			gl.glColor3f(0.60f, 0.65f, 0.72f);
			glu.gluCylinder(quadric, p[2], p[2] * 0.85, height, 32, 1);
			gl.glTranslated(0, 0, height);
			gl.glColor3f(0.78f, 0.82f, 0.88f);
			glu.gluDisk(quadric, 0, p[2] * 0.85, 32, 1);
			gl.glPopMatrix();
		}
	}

	/*
	 * The floor at z = 0 (the size: Floor Size): a dark surface (lighter in the
	 * middle), a grid every 100 units (stronger every 500), a blue edge and the soft
	 * shadows of the boids (larger and fainter the higher a boid flies).
	 */
	private void drawFloor(GL2 gl) {
		double h = floorHalf();
		double top = flightTop();
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_COLOR_BUFFER_BIT);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

		/* The surface: a fan of triangles, light in the centre, dark at the edge */
		gl.glBegin(GL2.GL_TRIANGLE_FAN);
		gl.glColor3f(0.20f, 0.25f, 0.32f);
		gl.glVertex3d(0, 0, 0);
		gl.glColor3f(0.06f, 0.08f, 0.11f);
		int sides = 16;
		for (int k = 0; k <= 4 * sides; k++) {
			/* Along the edge of the square (counter-clockwise) */
			double t = (k % sides) / (double) sides * 2 - 1;
			int side = (k / sides) % 4;
			double x = side == 0 ? h : side == 1 ? -t * h : side == 2 ? -h : t * h;
			double y = side == 0 ? t * h : side == 1 ? h : side == 2 ? -t * h : -h;
			gl.glVertex3d(x, y, 0);
		}
		gl.glEnd();

		/* The grid (transparent lines, without writing the depth) */
		gl.glDepthMask(false);
		gl.glLineWidth(1f);
		gl.glBegin(GL2.GL_LINES);
		int n = (int) Math.floor(h / 100);
		for (int k = -n; k <= n; k++) {
			double v = k * 100.0;
			boolean major = k % 5 == 0;
			gl.glColor4f(0.55f, 0.70f, 0.90f, major ? 0.32f : 0.10f);
			gl.glVertex3d(v, -h, 0.5);
			gl.glVertex3d(v, h, 0.5);
			gl.glVertex3d(-h, v, 0.5);
			gl.glVertex3d(h, v, 0.5);
		}
		gl.glEnd();

		/* The edge */
		gl.glLineWidth(2f);
		gl.glColor4f(0.30f, 0.56f, 0.95f, 0.9f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		gl.glVertex3d(h, h, 1);
		gl.glVertex3d(-h, h, 1);
		gl.glVertex3d(-h, -h, 1);
		gl.glVertex3d(h, -h, 1);
		gl.glEnd();

		/* The shadows of the boids: a soft disc below each boid (y inverted as the boids) */
		for (Boid b : boids) {
			double t = Math.max(0, Math.min(1, b.z / top));
			double r = radius * (2.4 + 2.5 * t);
			float alpha = (float) (0.5 * (1 - 0.7 * t));
			gl.glBegin(GL2.GL_TRIANGLE_FAN);
			gl.glColor4f(0f, 0f, 0f, alpha);
			gl.glVertex3d(b.x, -b.y, 1.5);
			gl.glColor4f(0f, 0f, 0f, 0f);
			for (int k = 0; k <= 16; k++) {
				double angle = k * Math.PI / 8;
				gl.glVertex3d(b.x + r * Math.cos(angle), -b.y + r * Math.sin(angle), 1.5);
			}
			gl.glEnd();
		}
		gl.glDepthMask(true);
		gl.glPopAttrib();
	}

   /*The following two methods will be API in the next release to call them easily!*/
	private void moveViewToPanel(final CustomView customView, final String viewId, final String panelLocation,
			String newTitle) {
		if (customView == null || viewId == null || panelLocation == null) {
			return;
		}

		// 1. Get the target composite from Bio7 and set the focus
		final Composite targetParent = customView.getComposite(panelLocation);
		if (targetParent == null) {
			System.err.println("Bio7 panel '" + panelLocation + "' could not be found.");
			return;
		}
		renamePanelTab(targetParent, newTitle);
		targetParent.setFocus();

		// 2. First open the view the classic way in the Eclipse background
		Work.openView(viewId);

		// 3. Defer to the UI thread asynchronously so the widgets have time to render
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (targetParent.isDisposed()) {
					return;
				}

				try {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IViewPart targetView = page.findView(viewId);

					if (targetView != null) {
						Control foundControl = null;

						// Strategy A: Try to call the view's standard control getter
						try {
							java.lang.reflect.Method getControlMethod = targetView.getClass().getMethod("getControl");
							foundControl = (Control) getControlMethod.invoke(targetView);
						} catch (Exception e) {
							// Ignore if not publicly declared
						}

						// Strategy B: If null, scan the view's internal fields for
						// initialized widgets
						if (foundControl == null) {
							java.lang.reflect.Field[] fields = targetView.getClass().getDeclaredFields();
							for (java.lang.reflect.Field field : fields) {
								if (Control.class.isAssignableFrom(field.getType())) {
									field.setAccessible(true);
									Object value = field.get(targetView);
									if (value != null) {
										foundControl = (Control) value;
										break;
									}
								}
							}
						}

						// Strategy C: Climb up recursively to the view's true root layout container
						if (foundControl != null && !foundControl.isDisposed()) {
							Control rootContainer = foundControl;

							/*
							 * Climb the widget tree upwards until we hit the official boundary of the
							 * Eclipse framework wrappers (tab folder structures).
							 */
							while (rootContainer.getParent() != null) {
								String parentClassName = rootContainer.getParent().getClass().getName();

								if (parentClassName.contains("org.eclipse.ui")
										|| parentClassName.contains("org.eclipse.e4")
										|| parentClassName.contains("ViewSite")) {
									break; // Stop right before the workbench wrappers
								}
								rootContainer = rootContainer.getParent();
							}

							// 4. REPARENTING: Change the native SWT parent to the Bio7 panel
							rootContainer.setParent(targetParent);

							// 5. Force a layout update so the elements adapt to the panel
							targetParent.layout(true, true);
							rootContainer.setVisible(true);

							// 6. Close the empty, unused Eclipse background tab of the original view
							page.hideView(targetView);
						} else {
							System.err.println("Could not locate the UI components of view '" + viewId + "'.");
						}
					}
				} catch (Exception e) {
					System.err.println("Error while moving view '" + viewId + "': " + e.getMessage());
					e.printStackTrace();
				}
			}
		});
	}

	private void renamePanelTab(final Composite panelComposite, final String newTitle) {
		if (panelComposite == null || panelComposite.isDisposed() || newTitle == null) {
			return;
		}

		// Climb up the widget tree until we reach the CTabFolder of the CustomView
		Composite parent = panelComposite.getParent();
		Control child = panelComposite;

		while (parent != null) {
			if (parent instanceof CTabFolder) {
				CTabFolder folder = (CTabFolder) parent;
				for (CTabItem item : folder.getItems()) {
					// Find the CTabItem whose control is our panel (or contains it)
					if (item.getControl() == child || item.getControl() == panelComposite) {
						item.setText(newTitle);
						return;
					}
				}
				// Fallback: if getControl() returns null for the items,
				// rename the currently selected tab instead
				if (folder.getSelection() != null) {
					folder.getSelection().setText(newTitle);
				}
				return;
			}
			// Also support the classic (non-custom) TabFolder
			if (parent instanceof TabFolder) {
				TabFolder folder = (TabFolder) parent;
				for (TabItem item : folder.getItems()) {
					if (item.getControl() == child || item.getControl() == panelComposite) {
						item.setText(newTitle);
						return;
					}
				}
				return;
			}
			child = parent;
			parent = parent.getParent();
		}

		System.err.println("No tab container found for the panel - title unchanged.");
	}
}
