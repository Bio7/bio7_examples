package opengl.additional;

import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class LorenzAttractor extends com.eco.bio7.compile.Model {
public int i,n=10000;
public double x0=0.1,y0=0,z0=0,x1,y1,z1;
public double h = 0.005;
public double a = 10.0;
public double b = 28.0;
public double c = 8.0 / 3.0;
   
public void run(GL2 gl, GLU glu, GLUT glut) {
    /*We disable the lightening to see the colour of the points primitives!*/
	gl.glDisable(GL_LIGHTING);

	gl.glColor3f(0.0f, 0.0f, 1.0f);
	gl.glPushMatrix();
	gl.glScalef(100.0f, 100.0f, 100.0f);// here you
	gl.glPointSize(5);
	gl.glBegin(GL_POINTS);
	for (i = 0; i <= n; i++) {
		x1 = x0 + h * a * (y0 - x0);
		y1 = y0 + h * (x0 * (b - z0) - y0);
		z1 = z0 + h * (x0 * y0 - c * z0);
		x0 = x1;
		y0 = y1;
		z0 = z1;
		if (i > 100)
			gl.glVertex3d((x0 - 0.95) / 5, (y0 - 1.78) / 5, (z0 - 26.7) / 5);
	}
	gl.glEnd();
	gl.glPopMatrix();
	gl.glEnable(GL_LIGHTING);

}
}