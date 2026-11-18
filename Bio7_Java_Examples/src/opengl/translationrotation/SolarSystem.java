package opengl.translationrotation;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;

/*
 This example demonstrates a solar system with all planets
 orbiting in the x,y plane using rotation and translation with the OpenGL API!
 Planets are scaled with realistic relative dimensions.
 */
public class SolarSystem extends com.eco.bio7.compile.Model {

	private float sunRotation = 0.1f;
	private float mercuryYear = 0.1f, mercuryDay = 0.1f;
	private float venusYear = 0.1f, venusDay = 0.1f;
	private float earthYear = 0.1f, earthDay = 0.1f;
	private float marsYear = 0.1f, marsDay = 0.1f;
	private float jupiterYear = 0.1f, jupiterDay = 0.1f;
	private float saturnYear = 0.1f, saturnDay = 0.1f;
	private float uranusYear = 0.1f, uranusDay = 0.1f;
	private float neptuneYear = 0.1f, neptuneDay = 0.1f;

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		if (SpatialUtil.isStarted() == false) {
			SpatialUtil.startStop();
		}
		SpatialUtil.setFullscreen();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		// Update rotation angles
		sunRotation = (sunRotation + 0.2f) % 360;
		mercuryYear = (mercuryYear + 0.4f) % 360;
		mercuryDay = (mercuryDay + 0.6f) % 360;
		venusYear = (venusYear + 0.15f) % 360;
		venusDay = (venusDay + 0.3f) % 360;
		earthYear = (earthYear + 0.1f) % 360;
		earthDay = (earthDay + 0.5f) % 360;
		marsYear = (marsYear + 0.08f) % 360;
		marsDay = (marsDay + 0.4f) % 360;
		jupiterYear = (jupiterYear + 0.02f) % 360;
		jupiterDay = (jupiterDay + 0.3f) % 360;
		saturnYear = (saturnYear + 0.01f) % 360;
		saturnDay = (saturnDay + 0.25f) % 360;
		uranusYear = (uranusYear + 0.005f) % 360;
		uranusDay = (uranusDay + 0.2f) % 360;
		neptuneYear = (neptuneYear + 0.003f) % 360;
		neptuneDay = (neptuneDay + 0.15f) % 360;

		// Draw Sun (radius: 109.2 times Earth) - scaled to 50 units
		gl.glColor3f(1.0f, 0.9f, 0.0f); // Yellow
		gl.glPushMatrix();
		gl.glRotatef((float) sunRotation, 0.0f, 0.0f, 1.0f);
		glut.glutSolidSphere(50.0f, 30, 24);
		gl.glPopMatrix();

		// Draw Mercury (radius: 0.38 times Earth) - scaled to 2.0 units
		gl.glColor3f(0.7f, 0.7f, 0.7f); // Gray
		gl.glPushMatrix();
		gl.glRotatef((float) mercuryYear, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(80.0f, 0.0f, 0.0f);
		gl.glRotatef((float) mercuryDay, 0.0f, 0.0f, 1.0f);
		glut.glutSolidSphere(2.0f, 12, 10);
		gl.glPopMatrix();

		// Draw Venus (radius: 0.95 times Earth) - scaled to 5.0 units
		gl.glColor3f(0.9f, 0.8f, 0.3f); // Yellow-Orange
		gl.glPushMatrix();
		gl.glRotatef((float) venusYear, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(120.0f, 0.0f, 0.0f);
		gl.glRotatef((float) venusDay, 0.0f, 0.0f, 1.0f);
		glut.glutSolidSphere(5.0f, 12, 10);
		gl.glPopMatrix();

		// Draw Earth (radius: 1.0 times Earth) - scaled to 5.3 units
		gl.glColor3f(0.2f, 0.5f, 0.9f); // Blue
		gl.glPushMatrix();
		gl.glRotatef((float) earthYear, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(170.0f, 0.0f, 0.0f);
		gl.glRotatef((float) earthDay, 0.0f, 0.0f, 1.0f);
		glut.glutSolidSphere(5.3f, 12, 10);
		gl.glPopMatrix();

		// Draw Mars (radius: 0.53 times Earth) - scaled to 2.8 units
		gl.glColor3f(0.8f, 0.4f, 0.2f); // Red
		gl.glPushMatrix();
		gl.glRotatef((float) marsYear, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(220.0f, 0.0f, 0.0f);
		gl.glRotatef((float) marsDay, 0.0f, 0.0f, 1.0f);
		glut.glutSolidSphere(2.8f, 12, 10);
		gl.glPopMatrix();

		// Draw Jupiter (radius: 11.21 times Earth) - scaled to 59.4 units
		gl.glColor3f(0.8f, 0.7f, 0.5f); // Brown-Orange
		gl.glPushMatrix();
		gl.glRotatef((float) jupiterYear, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(350.0f, 0.0f, 0.0f);
		gl.glRotatef((float) jupiterDay, 0.0f, 0.0f, 1.0f);
		glut.glutSolidSphere(59.4f, 20, 16);
		gl.glPopMatrix();

		// Draw Saturn (radius: 9.45 times Earth) - scaled to 50.1 units with rings
		gl.glPushMatrix();
		gl.glRotatef((float) saturnYear, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(500.0f, 0.0f, 0.0f);
		gl.glRotatef((float) saturnDay, 0.0f, 0.0f, 1.0f);
		
		// Draw Saturn planet
		gl.glColor3f(0.9f, 0.8f, 0.6f); // Pale Yellow
		glut.glutSolidSphere(50.1f, 20, 16);
		
		// Draw Saturn rings
		drawSaturnRings(gl, glu);
		
		gl.glPopMatrix();

		// Draw Uranus (radius: 4.01 times Earth) - scaled to 21.3 units
		gl.glColor3f(0.4f, 0.8f, 0.9f); // Cyan
		gl.glPushMatrix();
		gl.glRotatef((float) uranusYear, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(620.0f, 0.0f, 0.0f);
		gl.glRotatef((float) uranusDay, 0.0f, 0.0f, 1.0f);
		glut.glutSolidSphere(21.3f, 16, 12);
		gl.glPopMatrix();

		// Draw Neptune (radius: 3.88 times Earth) - scaled to 20.6 units
		gl.glColor3f(0.2f, 0.4f, 0.8f); // Deep Blue
		gl.glPushMatrix();
		gl.glRotatef((float) neptuneYear, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(740.0f, 0.0f, 0.0f);
		gl.glRotatef((float) neptuneDay, 0.0f, 0.0f, 1.0f);
		glut.glutSolidSphere(20.6f, 16, 12);
		gl.glPopMatrix();
	}

	private void drawSaturnRings(GL2 gl, GLU glu) {
		GLUquadric quadric = glu.gluNewQuadric();
		
		// Enable blending for transparency
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		
		// Draw ring as a disk
		gl.glColor4f(0.85f, 0.75f, 0.55f, 0.7f); // Tan with transparency
		gl.glPushMatrix();
		gl.glRotatef(20.0f, 1.0f, 0.0f, 0.0f); // Tilt the rings
		glu.gluDisk(quadric, 65.0f, 100.0f, 40, 5); // Inner radius, outer radius
		gl.glPopMatrix();
		
		// Disable blending
		gl.glDisable(GL2.GL_BLEND);
	}
}