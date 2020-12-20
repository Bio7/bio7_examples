package opengl.rdata;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rbridge.RServe;
import com.jogamp.opengl.util.gl2.GLUT;

import static com.eco.bio7.spatial.SpatialUtil.*;
import static com.jogamp.opengl.GL2.*; 

/*
This example creates random (normal distributed) numbers dynamically 
in R and transfers them to the Space view.
In this example we draw points primitives!
Rserve has to be alive!
*/

public class RDataVisualizationPointsPrimitive extends com.eco.bio7.compile.Model {
	

int[] x;
int[] y;
int[] z;
float []no_mat = { 0.0f, 0.0f, 0.0f, 1.0f };
float []fBrightLight = { 1.0f, 1.0f, 1.0f, 1.0f };

public void setup(GL2 gl, GLU glu, GLUT glut) {
	
	createRData();

}

public void createRData(){
	if (RServe.isAlive()) {
		try {
			/*Create the random data in R! This time we use 
			integers to save memory!*/
			try{
			RConnection connection = RServe.getConnection();
			x = (int[]) connection.eval("x<-as.integer(rnorm(100000)*100)").asIntegers();
			y = (int[]) connection.eval("y<-as.integer(rnorm(100000)*100)").asIntegers();
			z = (int[]) connection.eval("z<-as.integer(rnorm(100000)*100)").asIntegers();
			}
			catch (REXPMismatchException e) {
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
	//gl.glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL,GL.GL_SEPARATE_SPECULAR_COLOR);
	if (z != null) {

		for (int i = 0; i < z.length; i++) {
			/*Here we draw the spheres! We translate the points
			The next point is drawn from the origin (PushMatrix!!)!*/

			gl.glPushMatrix();
			//gl.glTranslated(x[i], y[i], z[i]);
			gl.glPointSize(1);
			
			gl.glDisable(GL_LIGHTING);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);	 	    					
	       /*Here we draw the points!*/
		    gl.glBegin(GL.GL_POINTS);	
		    /*We set the vertex with integers!*/
		    gl.glVertex3i(x[i],y[i],z[i]);
			gl.glEnd();
			gl.glEnable(GL_LIGHTING);
			gl.glPopMatrix();

		}

	}

}}