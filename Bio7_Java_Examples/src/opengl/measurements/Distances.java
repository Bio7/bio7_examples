package opengl.measurements;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import javax.vecmath.Point3d;

import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;

import static com.jogamp.opengl.GL2.*;

/*
 A simple example to calculate the euclidean distance and more 
 from different librarys between and to coordinates.
 */
public class Distances extends com.eco.bio7.compile.Model {
	float[] vert1 = { 100, 50, 0 };
	float[] vert2 = { 200, 200, 200 };

	double[] vert3 = { 100, 50, 0 };
	double[] vert4 = { 200, 0, 200 };

	public void run(GL2 gl, GLU glu, GLUT glut) {

		/* We disable the light for the line to see the colour! */
		gl.glDisable(GL_LIGHTING);
		gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glPushMatrix();
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(0, 0, 0);
		gl.glVertex3f(200, 200, 200);
		gl.glEnd();
		/* We paint a sphere! */
		gl.glTranslated(200, 200, 200);
		gl.glEnable(GL_LIGHTING);
		glut.glutSolidSphere(10, 10, 10);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(0, 0, 0);
		gl.glVertex3f(100, 50, 0);
		gl.glEnd();
		/* We paint a sphere! */
		gl.glTranslated(100, 50, 0);
		gl.glEnable(GL_LIGHTING);
		glut.glutSolidSphere(10, 10, 10);
		gl.glPopMatrix();
		/*
		 * The slider in the Time view triggers the canStep function to get an
		 * output of the measured distances!
		 */
		if (SpatialUtil.canStep()) {
			/* Methods from the javax.vecmath lib! */
			Point3d p3 = new Point3d(vert3);
			Point3d p4 = new Point3d(vert4);

			/* Methods from the javax.vecmath lib! */
			System.out.println("Euclidean distance(double):  "
					+ p3.distance(p4));
			System.out.println("Manhatten distance:  " + p3.distanceL1(p4));

		}
	}
}