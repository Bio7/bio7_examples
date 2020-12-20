package opengl.rdata;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RServe;
import com.jogamp.opengl.util.gl2.GLUT;
import static com.jogamp.opengl.GL2.*;

/*
 A simple example which draws a line plot with data from R.
 Invoke the setup method to see the result!
 */
public class RDataVisualization extends com.eco.bio7.compile.Model {

	double[] z;

	public void setup(GL2 gl, GLU glu, GLUT glut) {

		if (RServe.isAlive()) {
			try {

				/* We produce some random data with R! */
				RServe.getConnection().eval("z<-runif(500)*200");
				try {
					z = (double[]) RServe.getConnection().eval("z").asDoubles();
				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (RserveException e) {

				System.out.println(e.getRequestErrorDescription());
			}
		}

	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		gl.glDisable(GL_LIGHTING);
		if (z != null) {

			gl.glBegin(GL.GL_LINES);

			for (int i = 0; i < z.length; i = i + 1) {
				float zv = (float) z[i];

				if (i > 0) {
					float zvb = (float) z[i - 1];
					gl.glVertex3f(i, zvb, 0);
					gl.glVertex3f(i + 1, zv, 0);
				} else {
					gl.glVertex3f(i, 0, 0);
					gl.glVertex3f(i + 1, zv, 0);

				}
			}

			gl.glEnd();
		}
		gl.glEnable(GL_LIGHTING);
	}
}