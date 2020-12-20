package opengl.textureandmodels;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.spatial.SpatialLoader;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import static com.jogamp.opengl.GL2.*;

/*
 This example shows how to texture a cube.
 Use the setup and main  method to execute the example!

 !!!!!Please load the texture in the setup method to avoid a crash when using the fullscreen
 feature. See the manual for more details!
 */
public class TexturedCube extends com.eco.bio7.compile.Model {

	public Texture t;
	public float rot = 0f;
	public String fileroot;
	public String f;

	public void setup(GL2 gl, GLU glu, GLUT glut) {

		/* Create the path from the root of the workspace! */
		f = FileRoot.getCurrentCompileDir() + "/opengl/textureandmodels/FluorescentCells.jpg";
		/* Load the texture! */
		t = SpatialLoader.createTexture(f, gl);

	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		rot = rot + 0.01f;
		gl.glPushMatrix();
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

		if (t != null) {
			t.bind(gl);
			t.enable(gl);

			drawCube(gl);

			t.disable(gl);
		}
		gl.glPopMatrix();

	}

	/* Here we draw a cube and assign the texture on each face! */
	public void drawCube(GL2 gl) {
		gl.glBegin(GL_QUADS);
		// Front Face
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(-100.0f, -100.0f, 100.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(100.0f, -100.0f, 100.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(100.0f, 100.0f, 100.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(-100.0f, 100.0f, 100.0f);
		// Back Face
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(-100.0f, -100.0f, -100.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(-100.0f, 100.0f, -100.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(100.0f, 100.0f, -100.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(100.0f, -100.0f, -100.0f);
		// Top Face
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(-100.0f, 100.0f, -100.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(-100.0f, 100.0f, 100.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(100.0f, 100.0f, 100.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(100.0f, 100.0f, -100.0f);
		// Bottom Face
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(-100.0f, -100.0f, -100.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(100.0f, -100.0f, -100.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(100.0f, -100.0f, 100.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(-100.0f, -100.0f, 100.0f);
		// Right face
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(100.0f, -100.0f, -100.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(100.0f, 100.0f, -100.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(100.0f, 100.0f, 100.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(100.0f, -100.0f, 100.0f);
		// Left Face
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(-100.0f, -100.0f, -100.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(-100.0f, -100.0f, 100.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(-100.0f, 100.0f, 100.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(-100.0f, 100.0f, -100.0f);

		gl.glEnd();
	}
}