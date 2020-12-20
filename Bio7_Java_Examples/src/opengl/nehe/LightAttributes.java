package opengl.nehe;

import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
/*
 An example which adjusts some material and light attributes!
 Adapted from http://nehe.gamedev.net/
 */
import com.jogamp.opengl.glu.GLU;

import com.jogamp.opengl.util.gl2.GLUT;

public class LightAttributes extends com.eco.bio7.compile.Model {

	float[] no_mat = { 0.0f, 0.0f, 0.0f, 1.0f };
	float[] mat_ambient = { 0.7f, 0.7f, 0.7f, 1.0f };
	float[] mat_ambient_color = { 0.8f, 0.8f, 0.2f, 1.0f };
	float[] mat_diffuse = { 0.1f, 0.5f, 0.8f, 1.0f };
	float[] mat_specular = { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] no_shininess = { 0.0f };
	float[] low_shininess = { 5.0f };
	float[] high_shininess = { 100.0f };
	float[] mat_emission = { 0.3f, 0.2f, 0.2f, 0.0f };

	public void run(GL2 gl, GLU glu, GLUT glut) {

		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, no_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);

	}
}