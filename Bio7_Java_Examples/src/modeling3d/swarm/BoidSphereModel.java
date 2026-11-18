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
* Stop the "Play/Pause" action to recompile and press "Setup" again.
*/
package modeling3d.swarm;

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
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import cern.jet.random.tdouble.DoubleUniform;
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
	public float radius = 3.5f;
	public int FLOOR_LEN = 20; // should be even
	private final static int BLUE_TILE = 0; // floor tile colour types
	private final static int GREEN_TILE = 1;
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

		// Reset spatial rotations and set top-down perspective
		SpatialUtil.resetRotation();
		SpatialUtil.setRotationX(-90.0f);
		SpatialUtil.setSplitView(true);
		SpatialUtil.setCustomCamera(true);
		// Initialize Boid population
		boids = new ArrayList<Boid>();
		DoubleUniform uni = new DoubleUniform(-300, 300, twist);
		for (int i = 0; i < 200; i++) {
			boids.add(new Boid(uni.nextDouble(), uni.nextDouble(), (twist.nextDouble() - 0.5) * 20));
		}
		/* Start the run method! */
		if (!SpatialUtil.isStarted())
			SpatialUtil.startStop();

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

	public void run(GL2 gl, GLU glu, GLUT glut) {
		gl.glEnable(GL_COLOR_MATERIAL);
		/* Both sides of the wings lit (a glider is open, not a closed body) */
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
		gl.glEnable(GL2.GL_NORMALIZE);

		// Execute boid behavior and rendering
		for (Boid b : boids) {
			b.run(boids, gl, glut);
		} /* Add a boid if the mouse is double-clicked! */
		if (isMouseDoubleClicked()) {

			boids.add(new Boid(0, 0, 100));

		}
		gl.glScaled(100, 100, 100);
		gl.glRotated(90.0, 1.0, 0, 0);
		drawFloor(gl);

		// Camera tracking logic for the lead boid
		if (!boids.isEmpty()) {
			Boid target = boids.get(0);
			double speed = Math.sqrt(target.vx * target.vx + target.vy * target.vy);
			double dx = target.vx / speed;
			double dy = target.vy / speed;

			// Set camera using dynamic distance from GUI
			SpatialCamera.setCustomCamera(target.x - (dx * camDist), target.z + 10.0, target.y - (dy * camDist),
					target.x, target.z, target.y);
		}
	}

	/*
	 * The glider of a boid (nose along +x, wings in the x-y plane, the fin up +z),
	 * in units of the size ("Size" in the settings): a dart with a ridge on the
	 * top and the bottom, a notch at the tail and a fin.
	 */
	private static final double[] NOSE = { 2.2, 0, 0 }, LEFT = { -1.3, 1.7, 0 }, RIGHT = { -1.3, -1.7, 0 }, TAIL = { -0.7, 0, 0 },
			TOP = { -0.2, 0, 0.28 }, BOTTOM = { -0.2, 0, -0.22 }, FIN = { -1.25, 0, 0.95 };

	class Boid {
		double x, y, z, vx, vy, vz, ax, ay;
		/* The heading of the last frame and the bank (the roll into a turn, degrees) */
		double lastYaw = Double.NaN, bank;
		/* Each boid a shade of blue-cyan */
		final float[] color;

		Boid(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			java.awt.Color c = java.awt.Color.getHSBColor((float) (0.50 + Math.random() * 0.12), 0.75f, 0.95f);
			color = c.getRGBColorComponents(null);
			double angle = Math.random() * Math.PI * 2;
			this.vx = Math.cos(angle);
			this.vy = Math.sin(angle);
			this.vz = (Math.random() - 0.5) * 0.2;
		}

		void run(ArrayList<Boid> boids, GL2 gl, GLUT glut) {
			flock(boids);
			update();
			borders();
			render(gl, glut);
		}

		void flock(ArrayList<Boid> boids) {
			double[] sep = separate(boids);
			double[] ali = align(boids);
			double[] coh = cohesion(boids);

			// Weighting the steering forces using GUI variables
			ax += sep[0] * separationWeight + ali[0] * alignmentWeight + coh[0] * cohesionWeight;
			ay += sep[1] * separationWeight + ali[1] * alignmentWeight + coh[1] * cohesionWeight;
		}

		void update() {
			vx += ax;
			vy += ay;
			double s = Math.sqrt(vx * vx + vy * vy);
			// Limit speed based on dynamic GUI value
			if (s > maxSpeed) {
				vx = (vx / s) * maxSpeed;
				vy = (vy / s) * maxSpeed;
			}
			x += vx;
			y += vy;
			z += vz;
			if (Math.abs(z) > 1000)
				vz *= -1;
			ax = 0;
			ay = 0;
		}

		void render(GL2 gl, GLUT glut) {
			/* The direction of the flight (y inverted as the position) */
			double dx = vx, dy = -vy, dz = vz;
			double yaw = Math.toDegrees(Math.atan2(dy, dx));
			double pitch = Math.toDegrees(Math.atan2(dz, Math.hypot(dx, dy)));
			/* Banking: the glider rolls into a turn (smoothed) */
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
		private void triangle(GL2 gl, double[] a, double[] b, double[] c) {
			double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
			double wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
			gl.glNormal3d(uy * wz - uz * wy, uz * wx - ux * wz, ux * wy - uy * wx);
			gl.glVertex3d(a[0], a[1], a[2]);
			gl.glVertex3d(b[0], b[1], b[2]);
			gl.glVertex3d(c[0], c[1], c[2]);
		}

		// --- Steering Logic (Separate, Align, Cohesion) ---
		double[] separate(ArrayList<Boid> boids) {
			double sx = 0, sy = 0;
			int count = 0;
			for (Boid o : boids) {
				double d = Math.sqrt(Math.pow(x - o.x, 2) + Math.pow(y - o.y, 2));
				if (d > 0 && d < 30.0) {
					sx += (x - o.x) / d;
					sy += (y - o.y) / d;
					count++;
				}
			}
			return (count > 0) ? limitForce(sx / count, sy / count) : new double[] { 0, 0 };
		}

		double[] align(ArrayList<Boid> boids) {
			double sx = 0, sy = 0;
			int count = 0;
			for (Boid o : boids) {
				double d = Math.sqrt(Math.pow(x - o.x, 2) + Math.pow(y - o.y, 2));
				if (d > 0 && d < 60.0) {
					sx += o.vx;
					sy += o.vy;
					count++;
				}
			}
			return (count > 0) ? limitForce(sx / count, sy / count) : new double[] { 0, 0 };
		}

		double[] cohesion(ArrayList<Boid> boids) {
			double sx = 0, sy = 0;
			int count = 0;
			for (Boid o : boids) {
				double d = Math.sqrt(Math.pow(x - o.x, 2) + Math.pow(y - o.y, 2));
				if (d > 0 && d < 60.0) {
					sx += o.x;
					sy += o.y;
					count++;
				}
			}
			return (count > 0) ? seek(sx / count, sy / count) : new double[] { 0, 0 };
		}

		double[] seek(double tx, double ty) {
			double dx = tx - x, dy = ty - y, d = Math.sqrt(dx * dx + dy * dy);
			return (d > 0) ? limitForce((dx / d) * maxSpeed, (dy / d) * maxSpeed) : new double[] { 0, 0 };
		}

		double[] limitForce(double fx, double fy) {
			double mag = Math.sqrt(fx * fx + fy * fy);
			if (mag > 0) {
				fx = (fx / mag) * maxSpeed - vx;
				fy = (fy / mag) * maxSpeed - vy;
				double fMag = Math.sqrt(fx * fx + fy * fy);
				// Limit steering force based on dynamic GUI value
				if (fMag > maxForce) {
					fx = (fx / fMag) * maxForce;
					fy = (fy / fMag) * maxForce;
				}
			}
			return new double[] { fx, fy };
		}

		void borders() {
			if (x < -1000)
				x = 1000;
			if (x > 1000)
				x = -1000;
			if (y < -1000)
				y = 1000;
			if (y > 1000)
				y = -1000;
		}
	}
	/*
	 * Example from the book "Pro Java 6 3D Game Development" from Andrew Davison to
	 * draw a Checkerboard with text.
	 */

	private void drawFloor(GL2 gl)
	/*
	 * Create tiles, the origin marker, then the axes labels. The tiles are in a
	 * checkboard pattern, alternating between green and blue.
	 */
	{
		gl.glDisable(GL2.GL_LIGHTING);

		drawTiles(BLUE_TILE, gl); // blue tiles
		drawTiles(GREEN_TILE, gl); // green

		gl.glEnable(GL2.GL_LIGHTING);
	} // end of CheckerFloor()

	private void drawTiles(int drawType, GL2 gl)
	/*
	 * Create a series of quads, all with the same colour. They are spaced out over
	 * a FLOOR_LEN*FLOOR_LEN area, with the area centered at (0,0) on the XZ plane,
	 * and y==0.
	 */
	{
		if (drawType == BLUE_TILE)
			gl.glColor3f(0.0f, 0.1f, 0.4f);
		else
			// green
			gl.glColor3f(0.0f, 0.5f, 0.1f);

		gl.glBegin(GL2.GL_QUADS);
		boolean aBlueTile;
		for (int z = -FLOOR_LEN / 2; z <= (FLOOR_LEN / 2) - 1; z++) {
			aBlueTile = (z % 2 == 0) ? true : false; // set colour type for new row
			for (int x = -FLOOR_LEN / 2; x <= (FLOOR_LEN / 2) - 1; x++) {
				if (aBlueTile && (drawType == BLUE_TILE)) // blue tile and drawing blue
					drawTile(x, z, gl);
				else if (!aBlueTile && (drawType == GREEN_TILE)) // green
					drawTile(x, z, gl);
				aBlueTile = !aBlueTile;
			}
		}
		gl.glEnd();
	} // end of drawTiles()

	private void drawTile(int x, int z, GL2 gl)
	/*
	 * Coords for a single blue or green square; its top left hand corner at
	 * (x,0,z).
	 */
	{
		// points created in counter-clockwise order
		gl.glVertex3f(x, 0.0f, z + 1.0f); // bottom left point
		gl.glVertex3f(x + 1.0f, 0.0f, z + 1.0f);
		gl.glVertex3f(x + 1.0f, 0.0f, z);
		gl.glVertex3f(x, 0.0f, z);
	} // end of drawTile()
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
