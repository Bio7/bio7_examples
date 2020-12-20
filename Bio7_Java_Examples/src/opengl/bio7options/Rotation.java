package opengl.bio7options;

/*
 This example shows how to rotate the default view!
 */
import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.gl2.GLUT;


public class Rotation extends com.eco.bio7.compile.Model {
	public float rot = 0.0f;

	public void run(GL2 gl, GLU glu, GLUT glut) {

		rot = (rot + 0.1f) % 360;// One rotation and starts with 0.0f!
		SpatialUtil.setRotationZ(rot);
		// setRotationX(rot);
		// setRotationY(rot);
		// System.out.println(getRotationZ());
	}
}