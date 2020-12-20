package opengl.translationrotation;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;

/*
 This example demonstrates the use of rotation and translation
 with the OpenGL API!
 */
public class PlanetsOpenGL extends com.eco.bio7.compile.Model {

	private float year = 0.1f, year2 = 0.1f, day = 0.1f, hour = 0.1f;

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		if (SpatialUtil.isStarted() == false) {
			SpatialUtil.startStop();
		}
		SpatialUtil.setFullscreen();

	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		year2 = (year2 + 0.05f) % 360;
		year = (year + 0.1f) % 360;
		day = (day + 0.3f) % 360;
		hour = (hour + 0.5f) % 360;

		gl.glColor3f(1.0f, 1.0f, 1.0f);

		gl.glPushMatrix();
		gl.glRotatef((float) year2, 0.0f, 0.0f, 1.0f);
		/* Draw a Sphere! */
		glut.glutWireSphere(100.0, 20, 16);

		gl.glRotatef((float) year, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(200.0f, 0.0f, 0.0f);
		gl.glRotatef((float) day, 0.0f, 0.0f, 1.0f);
		/* Draw a Sphere! */
		glut.glutWireSphere(30.0f, 10, 10);

		gl.glPushMatrix();
		gl.glTranslatef(50.0f, 0.0f, 0.0f);
		gl.glRotatef((float) day, 0.0f, 0.0f, 1.0f);
		/* Draw a Sphere! */
		glut.glutWireSphere(10.0f, 10, 10);

		gl.glPopMatrix();
		gl.glPopMatrix();

	}
}