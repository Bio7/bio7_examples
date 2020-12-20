package opengl.additional;

import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;
/*
This examples draws an animated grid which moves up and down!
*/
public class Grid extends com.eco.bio7.compile.Model {
public double i=0;
public boolean sw=false;
public float rot=0.0f;

public void run(GL2 gl, GLU glu, GLUT glut) {
	// Please enter your OpenGL code here
	rot = (rot + 0.1f) % 360;//One rotation and starts with 0.0f!
	SpatialUtil.setRotationZ(rot);
	gl.glDisable(GL_LIGHTING);
	gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
	gl.glTranslated(0, 0, i);
	if (sw == false) {
		if (i < 500) {

			i = i + 1;
		} else {
			sw = true;
		}
	} else {
		if (sw) {
			if (i > -500) {
				i = i - 1;
			} else {
				sw = false;
			}
		}
	}
   /*Here we begin to draw the lines!*/
	gl.glBegin(GL_LINES);

	/* Horizontal lines. */
	for (int i = -500; i <= 500; i = i + 100) {
		gl.glVertex2f(-500, i);
		gl.glVertex2f(500, i);
	}
	/* Vertical lines. */
	for (int i = -500; i <= 500; i = i + 100) {
		gl.glVertex2f(i, -500);
		gl.glVertex2f(i, 500);
	}
	gl.glEnd();
}
}