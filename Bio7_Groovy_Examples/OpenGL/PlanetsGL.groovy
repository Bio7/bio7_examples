/*
This examples demonstrates the use of the Java 
OpenGL API with Groovy! Press the setup action
to start the animation in Fullscreen!
*/
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.eco.bio7.compile.*;
import com.eco.bio7.loader3d.OBJModel;
import com.eco.bio7.spatial.SpatialCamera;
import com.eco.bio7.spatial.SpatialEvents;
import com.eco.bio7.spatial.SpatialLoader;
import com.eco.bio7.spatial.SpatialMath;
import com.eco.bio7.spatial.SpatialPicking;
import com.eco.bio7.spatial.SpatialUtil;


class Model extends com.eco.bio7.compile.Model {

	def year = 0.1f, year2 = 0.1f, day = 0.1f, hour = 0.1f;
    /*A setup method to start the animation loop and set the fullscreen!*/
	def void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		if (SpatialUtil.isStarted() == false) {
			SpatialUtil.startStop();
		}
		//SpatialUtil.setFullscreen();

	}

	def void run(GL2 gl, GLU glu, GLUT glut) {
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

def m=new Model();//Instantiate the Model class!
Compiled.setModel(m);//Assign the Object!
m.setup();//Call the setup method!

