package opengl.bio7options;

/*
 You can resize or locate the splitscreen with this method dynamically!
 */
import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;


public class SplitScreenSize extends com.eco.bio7.compile.Model {
	public void setup(GL2 gl, GLU glu, GLUT glut) {
		// Please enter your Java OpenGL setup code here

	}

	public void run(GL2 gl, GLU glu, GLUT glut) {

		SpatialUtil.setSplitScreenSizeLocation(0, 500, 100, 100);

	}
}