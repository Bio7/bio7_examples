package opengl.clipping;

import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;


public class ClippingPlanes extends com.eco.bio7.compile.Model {
	/*
	 * This example demonstrates the use of clipping planes!
	 */

	public void run(GL2 gl, GLU glu, GLUT glut) {
		// Please enter your OpenGL code here
		double eqn[] = { 0.0, 1.0, 0.0, 0.0 };
		double eqn2[] = { 1.0, 0.0, 0.0, 0.0 };

		gl.glClear(GL_COLOR_BUFFER_BIT);

		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glPushMatrix();
		gl.glTranslatef(0.0f, 0.0f, -5.0f);

		/* clip lower half -- y < 0 */
		gl.glClipPlane(GL_CLIP_PLANE0, eqn, 0);
		gl.glEnable(GL_CLIP_PLANE0);
		/* clip left half -- x < 0 */
		gl.glClipPlane(GL_CLIP_PLANE1, eqn2, 0);
		gl.glEnable(GL_CLIP_PLANE1);

		gl.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
		glut.glutWireSphere(100.0, 20, 16);

		/* Disable clipping planes to turn back to the normal mode! */
		gl.glDisable(GL_CLIP_PLANE0);
		gl.glDisable(GL_CLIP_PLANE1);
		gl.glPopMatrix();
	}
}