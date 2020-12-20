package opengl.pointsandquads;

import static com.eco.bio7.spatial.SpatialUtil.*;
import cern.jet.random.tfloat.FloatUniform;
import cern.jet.random.tfloat.engine.FloatMersenneTwister;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import static com.jogamp.opengl.GL2.*;

/*
 This example draws spheres randomly!
 */

public class SpherePoints extends com.eco.bio7.compile.Model {

	public int random;
	private FloatUniform uni = new FloatUniform(0.0f, 1000.0f,
			new FloatMersenneTwister(new java.util.Date()));
	public double[] x = new double[1000];
	public double[] y = new double[1000];
	public double[] z = new double[1000];
	public double[] c1 = new double[1000];
	public double[] c2 = new double[1000];
	public double[] c3 = new double[1000];
	public float[] no_mat = { 0.0f, 0.0f, 0.0f, 1.0f };
	public float[] fBrightLight = { 1.0f, 1.0f, 1.0f, 1.0f };
	public int rand;

	public void run(GL2 gl, GLU glu, GLUT glut) {

		/* We enable a specular light modus in OpenGL! */
		gl.glLightModeli(GL_LIGHT_MODEL_COLOR_CONTROL,
				GL_SEPARATE_SPECULAR_COLOR);
		/*
		 * gl.glLightModeli( GL.GL_LIGHT_MODEL_COLOR_CONTROL, GL.GL_SINGLE_COLOR
		 * );
		 */

		for (int i = 0; i < 1000; i++) {

			/* Here we use the canStep method from the control panel! */
			if (canStep()) {
				/* We randomize the coordinates at each step! */
				x[i] = uni.nextFloat();
				y[i] = uni.nextFloat();
				z[i] = uni.nextFloat();
				/* We randomize the colour! */
				c1[i] = Math.random();
				c2[i] = Math.random();
				c3[i] = Math.random();
			}

			gl.glPushMatrix();

			gl.glTranslated(x[i], y[i], z[i]);
			gl.glColor4d(c1[i], c2[i], c3[i], 1.0f);
			gl.glEnable(GL_COLOR_MATERIAL);
			gl.glColorMaterial(GL.GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
			gl.glMaterialfv(GL.GL_FRONT, GL_SPECULAR, fBrightLight, 0);

			gl.glMateriali(GL.GL_FRONT, GL_SHININESS, 110);

			glut.glutSolidSphere(10, 8, 8);

			gl.glPopMatrix();

		}
	}
}