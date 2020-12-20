package opengl.camera;

import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_COLOR_MATERIAL;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_SPECULAR;
import java.util.Date;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import cern.jet.random.tdouble.DoubleUniform;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;
import com.eco.bio7.spatial.SpatialCamera;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;

public class IndividualRace extends com.eco.bio7.compile.Model {
	/*
	 * The example demonstrates the use of a custom camera and tracks the motion
	 * of a sphere! Please invoke the setup method for a correct view. Enable
	 * the split view to see the result of a custom camera!
	 */
	public DoubleMersenneTwister twist = new DoubleMersenneTwister(new Date());
	public DoubleUniform uni = new DoubleUniform(150.0, 350.0, twist);
	public DoubleUniform uni2 = new DoubleUniform(0.0, 360.0, twist);

	public float[] no_mat = { 0.0f, 0.0f, 0.0f, 1.0f };
	public int counter = 0;
	public double last = 100;
	public double angle = 45;
	public double xStep = 0;
	public double yStep = 0;
	public double zStep = 0;
	public double xpos = 0;
	public double ypos = 0;
	public double zpos = 0;

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		/* We care about the correct rotation values! */

		/* Reset the user interface rotation! */
		SpatialUtil.resetRotation();
		/* Reset the world rotation! */
		SpatialUtil.setRotationX(0.0f);
		SpatialUtil.setRotationY(0.0f);
		SpatialUtil.setRotationZ(0.0f);
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		/* Here we define the camera view! */
		SpatialCamera.setCustomCamera(xpos, ypos - 160, 100, xpos, ypos, 0);

		/* We adjust some random movements! */
		xStep = Math.cos(Math.toRadians(angle));
		yStep = Math.sin(Math.toRadians(angle));
		zStep = Math.sin(Math.toRadians(angle));
		xpos = xpos + xStep;
		ypos = ypos + yStep;

		counter++;
		if (counter > last) {
			last = uni.nextDouble();
			counter = 0;
			angle = uni2.nextDouble();
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
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL_SPECULAR, no_mat, 0);
		gl.glTranslated(xpos, ypos, zpos);
		glut.glutSolidSphere(30, 20, 20);

	}
}