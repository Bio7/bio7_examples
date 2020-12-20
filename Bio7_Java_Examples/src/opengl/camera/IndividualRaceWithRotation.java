package opengl.camera;

import java.util.Date;
import cern.jet.random.tdouble.DoubleUniform;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;
import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.spatial.SpatialCamera;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;


public class IndividualRaceWithRotation extends com.eco.bio7.compile.Model {
	/*
	 * The example demonstartes the use of a custom camera and tracks the motion
	 * and direction of a sphere! Please invoke the setup method!
	 */
	public DoubleMersenneTwister twist = new DoubleMersenneTwister(new Date());
	public DoubleUniform uni = new DoubleUniform(150.0, 350.0, twist);
	public DoubleUniform uni2 = new DoubleUniform(-5.0, 5.0, twist);

	float[] no_mat = { 0.0f, 0.0f, 0.0f, 1.0f };
	float[] fBrightLight = { 1.0f, 1.0f, 1.0f, 1.0f };
	int counter = 0;
	double last = 100;
	double angle = 45;
	double xStep = 0;
	double yStep = 0;
	double zStep = 0;
	double xpos = 0;
	double ypos = 0;
	double zpos = 0;
	float rotatz = 0.5f;
	int time = 0;

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		/* We care about the correct rotation values! */

		/* Reset the user interface rotation! */
		SpatialUtil.resetRotation();
		/* Adjust the world rotation! */
		SpatialUtil.setRotationX(-90.0f);
		SpatialUtil.setRotationY(0.0f);
		SpatialUtil.setRotationZ(0.0f);
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		// Please enter your OpenGL code here

		rotatz += 0.5f;

		angle = rotatz;

		xStep = Math.sin(Math.toRadians(angle));
		yStep = Math.cos(Math.toRadians(angle));
		zStep = Math.cos(Math.toRadians(angle));
		xpos = xpos + xStep;
		ypos = ypos + yStep;
		zpos = zpos + zStep;
		counter++;
		if (counter > last) {
			last = uni.nextDouble();
			counter = 0;

		}

		if (xpos > 500) {

			xpos = 500;
		} else if (xpos < -500) {
			xpos = -500;
		} else if (ypos > 500) {
			ypos = 500;

		} else if (ypos < -500) {
			ypos = -500;

		} else if (zpos > 500) {
			zpos = 500;

		} else if (zpos < -500) {
			zpos = -500;

		}

		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColor4d(0.5, 0.2, 0.6, 1.0f);
		gl.glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR, no_mat, 0);

		gl.glTranslated(xpos, -ypos, 0);
		// glut.glutSolidSphere(10, 20, 20);

		/* We rotate and translate the partikel in front of the camera position! */
		gl.glRotatef((float) angle, 0.0f, 0.0f, 1.0f);
		gl.glPushMatrix();
		gl.glTranslated(0, -60, 0);

		gl.glColor4d(0.0, 0.2, 0.6, 1.0f);
		gl.glMateriali(GL_FRONT, GL_SHININESS, 200);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, fBrightLight, 0);
		glut.glutSolidSphere(3, 40, 40);
		gl.glPopMatrix();
		/*
		 * We adjust the camera coordinates but this time have to consider that
		 * we rotated the world (z-achsis pointing up!). The camera coordinates
		 * are independant from the rotation.
		 */
		SpatialCamera.setCustomCamera(xpos, 11, ypos, xpos + (100 * xStep), 0,
				ypos + (100 * yStep));

	}
}