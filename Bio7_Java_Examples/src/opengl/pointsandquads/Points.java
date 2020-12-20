package opengl.pointsandquads;

import static com.eco.bio7.spatial.SpatialUtil.*;
import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class Points extends com.eco.bio7.compile.Model {

	double[] x = new double[100000];
	double[] y = new double[100000];
	double[] z = new double[100000];

	public void run(GL2 gl, GLU glu, GLUT glut) {

		for (int i = 0; i < 100000; i++) {

			/* Here we use the canStep method from the control panel! */
			if (canStep()) {
				/* We produce some random data! */
				x[i] = Math.random() * 1000;
				y[i] = Math.random() * 1000;
				z[i] = Math.random() * 1000;
			}
			/* For the points we have to disable the lights to see the colours! */
			gl.glDisable(GL_LIGHTING);
			gl.glPointSize(2);
			gl.glColor4f(0.0f, 0.5f, 0.8f, 1.0f);

			/* Here we draw the points! */
			gl.glBegin(GL.GL_POINTS);

			gl.glVertex3d(x[i], y[i], z[i]);

			gl.glEnd();
			/* We switch the light on again! */
			gl.glEnable(GL_LIGHTING);

		}

	}
}
