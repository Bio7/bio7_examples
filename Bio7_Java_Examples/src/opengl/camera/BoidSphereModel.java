/*
* The example demonstrates the use of a custom camera and tracks the motion
* of a sphere in a swarm model! Please invoke the setup method in the "Options Space" view for a correct display.
* Then Invoke the Play/Pause action to draw the OpenGL commands.
* Enable the "Custom Camera" view option in the "Camera" tab (optional the "Split View" option) 
* to see the result of a custom camera!
* 
* Stop the "Play/Pause" action to recompile and press "Setup" again.
*/
package opengl.camera;

import java.util.ArrayList;
import java.util.Date;
import cern.jet.random.tdouble.DoubleUniform;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;
import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.spatial.SpatialCamera;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;

public class BoidSphereModel extends com.eco.bio7.compile.Model {

	private ArrayList<Boid> boids;
	public DoubleMersenneTwister twist = new DoubleMersenneTwister(new Date());

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		SpatialUtil.resetRotation();
		/* Bio7 Setup: Z is the height */
		SpatialUtil.setRotationX(-90.0f);
		SpatialUtil.setRotationY(0.0f);
		SpatialUtil.setRotationZ(0.0f);

		boids = new ArrayList<Boid>();
		DoubleUniform uni = new DoubleUniform(-300, 300, twist);
		for (int i = 0; i < 200; i++) {
			boids.add(new Boid(uni.nextDouble(), uni.nextDouble(), (twist.nextDouble() - 0.5) * 20));
		}
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		gl.glEnable(GL_COLOR_MATERIAL);

		for (Boid b : boids) {
			b.run(boids, gl, glut);
		}

		// Camera follows the Sphere!
		if (!boids.isEmpty()) {
			Boid target = boids.get(0);
			double speed = Math.sqrt(target.vx * target.vx + target.vy * target.vy);
			double dx = target.vx / speed;
			double dy = target.vy / speed;

			double dist = 20.0;
			double heightOffset = 10.0;

			SpatialCamera.setCustomCamera(target.x - (dx * dist), target.z + heightOffset, // Camera also moves up and
																							// down!
					target.y - (dy * dist), target.x, target.z, target.y);
		}
	}

	class Boid {
		double x, y, z;
		double vx, vy, vz;
		double ax, ay;
		double maxspeed = 6.2;
		double maxforce = 0.05;
		float radius = 3.5f;

		Boid(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			double angle = Math.random() * Math.PI * 2;
			this.vx = Math.cos(angle);
			this.vy = Math.sin(angle);
			this.vz = (Math.random() - 0.5) * 0.2; // Individual start vertical velocity
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
			ax += sep[0] * 1.5 + ali[0] * 1.0 + coh[0] * 1.0;
			ay += sep[1] * 1.5 + ali[1] * 1.0 + coh[1] * 1.0;
		}

		void update() {
			vx += ax;
			vy += ay;
			double s = Math.sqrt(vx * vx + vy * vy);
			if (s > maxspeed) {
				vx = (vx / s) * maxspeed;
				vy = (vy / s) * maxspeed;
			}

			x += vx;
			y += vy;

			// Vertical Movement up/down!
			z += vz;
			if (Math.abs(z) > 1000)
				vz *= -1; // Return at 60 units height/depth!

			ax = 0;
			ay = 0;
		}

		// --- Boid-Logic (X/Y layer) ---
		double[] separate(ArrayList<Boid> boids) {
			double sx = 0, sy = 0;
			int count = 0;
			for (Boid o : boids) {
				double d = Math.sqrt(Math.pow(x - o.x, 2) + Math.pow(y - o.y, 2));
				if (d > 0 && d < 30.0) {
					double dx = x - o.x;
					double dy = y - o.y;
					sx += dx / d;
					sy += dy / d;
					count++;
				}
			}
			if (count > 0) {
				sx /= count;
				sy /= count;
			}
			return limitForce(sx, sy);
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
			if (count > 0)
				return limitForce(sx / count, sy / count);
			return new double[] { 0, 0 };
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
			if (count > 0)
				return seek(sx / count, sy / count);
			return new double[] { 0, 0 };
		}

		double[] seek(double tx, double ty) {
			double dx = tx - x;
			double dy = ty - y;
			double d = Math.sqrt(dx * dx + dy * dy);
			return limitForce((dx / d) * maxspeed, (dy / d) * maxspeed);
		}

		double[] limitForce(double fx, double fy) {
			double mag = Math.sqrt(fx * fx + fy * fy);
			if (mag > 0) {
				fx = (fx / mag) * maxspeed - vx;
				fy = (fy / mag) * maxspeed - vy;
				double fMag = Math.sqrt(fx * fx + fy * fy);
				if (fMag > maxforce) {
					fx = (fx / fMag) * maxforce;
					fy = (fy / fMag) * maxforce;
				}
			}
			return new double[] { fx, fy };
		}

		void render(GL2 gl, GLUT glut) {
			gl.glPushMatrix();
			gl.glTranslated(x, -y, z); // Bio7 transformation (Y inverted)
			gl.glColor3f(0.2f, 0.5f, 1.0f);
			glut.glutSolidSphere(radius, 20, 20); // Draw a simple sphere!
			gl.glPopMatrix();
		}

		void borders() {
			if (x < -1000)
				x = 1000;
			if (y < -1000)
				y = 1000;
			if (x > 1000)
				x = -1000;
			if (y > 1000)
				y = -1000;
		}
	}
}
