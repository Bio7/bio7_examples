package opengl.rdata;

import static com.eco.bio7.spatial.SpatialUtil.*;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rbridge.RServe;
import com.jogamp.opengl.util.gl2.GLUT;

import static com.jogamp.opengl.GL2.*;

/*
 This example creates random (normal distributed) numbers dynamically 
 in R and transfers them to the Space view!
 Rserve has to be alive!
 */
public class RDataVisualizationPoints extends com.eco.bio7.compile.Model {

	public double[] x;
	public double[] y;
	public double[] z;
	public float[] no_mat = { 0.0f, 0.0f, 0.0f, 1.0f };
	public float[] fBrightLight = { 1.0f, 1.0f, 1.0f, 1.0f };

	public void setup(GL gl, GLU glu, GLUT glut) {

		createRData();

	}

	public void createRData() {
		if (RServe.isAlive()) {
			try {
				/* Create the data in R! */
				RConnection connection = RServe.getConnection();
				connection.eval("x<-rnorm(1000)*300");
				connection.eval("y<-rnorm(1000)*300");
				connection.eval("z<-rnorm(1000)*300");
				/* Transfer the data from R! */
				try {
					x = (double[]) connection.eval("x").asDoubles();
					y = (double[]) connection.eval("y").asDoubles();
					z = (double[]) connection.eval("z").asDoubles();
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
		if (canStep()) {
			createRData();
		}
		gl.glLightModeli(GL_LIGHT_MODEL_COLOR_CONTROL,
				GL_SEPARATE_SPECULAR_COLOR);
		if (z != null) {

			for (int i = 0; i < z.length; i++) {
				/*
				 * Here we draw the spheres! The next point is drawn from the
				 * origin (PushMatrix)!
				 */

				gl.glPushMatrix();

				/* We translate the points from the origin! */
				gl.glTranslated(x[i], y[i], z[i]);

				gl.glEnable(GL_COLOR_MATERIAL);

				gl.glColor4f(0.5f, 0.5f, 0.8f, 1.0f);

				gl.glEnable(GL_COLOR_MATERIAL);
				gl.glColorMaterial(GL.GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
				gl.glMaterialfv(GL.GL_FRONT, GL_SPECULAR, fBrightLight, 0);
				gl.glMateriali(GL.GL_FRONT, GL_SHININESS, 110);
				/* Here we draw the sphere! */
				glut.glutSolidSphere(20, 6, 6);

				gl.glEnd();

				gl.glPopMatrix();

			}

		}

	}
}