package worldwind;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.collection.Work;
import com.eco.bio7.worldwind.*;
import com.jogamp.opengl.util.gl2.GLUT;

/*
 Simple startup for a dragged file!
 */
public class WorldWindStartup extends com.eco.bio7.compile.Model {

	public void setup() {
		Work.openPerspective("com.eco.bio7.WorldWind.3dglobe");
		WorldWindView.setFullscreen();
	}

	public void setup(GL gl, GLU glu, GLUT glut) {

	}

	public void run(GL gl, GLU glu, GLUT glut) {

	}
}
