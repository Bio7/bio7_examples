package opengl.collada;

import jcollada.*;
import java.io.File;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;


/*
Please install the Collada library in the JRE and recalculate the Java classpath
in the context menu of the Bio7 Navigator view!
Website: https://github.com/Meanz/jcollada
This snippet shows how to load a collada model in the 3d view.
Invoke the setup method to load the model! 
Please reference your collada model with the Bio7 paths!
*/
public class ColladaModel extends com.eco.bio7.compile.Model {
public DAE scene;
double time=0;
 
public void setup(GL gl,GLU glu,GLUT glut){
 
 scene = DAE.load(gl, new File("YourFile.dae"), new DAELoadConfig()); 

}
public void run(GL gl, GLU glu, GLUT glut) {
	if (scene != null) {
		//scene.applyCam(gl);
		scene.applyLights(gl);
		scene.draw(gl, time);
	}
	
		time = time + 0.02;
	
}
}