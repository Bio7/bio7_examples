/*
* The example demonstrates the use of a custom camera and tracks the motion
* of a sphere in a swarm model! Please invoke the setup method in the "Options Space" view for a correct display.
* Then Invoke the Play/Pause action to draw the OpenGL commands.
* Enable the "Custom Camera" view option in the "Camera" tab (optional the "Split View" option) 
* to see the result of a custom camera!
* 
* This example comes with a "Settings" view to adjust some Boid parameters.
* 
* Stop the "Play/Pause" action to recompile and press "Setup" again.
*/
package modeling3d;

import java.util.ArrayList;
import java.util.Date;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.collection.CustomView;
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

	public BoidSphereModel() {

	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		// Initialize the custom SWT GUI
		createGui();

		// Reset spatial rotations and set top-down perspective
		SpatialUtil.resetRotation();
		SpatialUtil.setRotationX(-90.0f);

		// Initialize Boid population
		boids = new ArrayList<Boid>();
		DoubleUniform uni = new DoubleUniform(-300, 300, twist);
		for (int i = 0; i < 200; i++) {
			boids.add(new Boid(uni.nextDouble(), uni.nextDouble(), (twist.nextDouble() - 0.5) * 20));
		}
	}

	private void createGui() {
		CustomView view = new CustomView();
		Display d = Display.getDefault();
		d.syncExec(() -> {
			// Create a tab in Bio7's Custom View
			Composite parent = view.getComposite("Boid Control Panel", null);
			parent.setLayout(new FillLayout());
			// Instantiate our manual GUI class
			new Settings(parent, BoidSphereModel.this);
			parent.layout(true);
		});
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		gl.glEnable(GL_COLOR_MATERIAL);

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

	class Boid {
		double x, y, z, vx, vy, vz, ax, ay;

		Boid(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
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
			gl.glPushMatrix();
			gl.glTranslated(x, -y, z); // Bio7 Y-Inversion
			gl.glColor3f(0.2f, 0.5f, 1.0f);
			glut.glutSolidSphere(radius, 16, 16);
			gl.glPopMatrix();
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

}
