package opengl.nehe;

import java.util.Date;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import static com.eco.bio7.spatial.SpatialUtil.*;
import cern.jet.random.tdouble.DoubleUniform;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;
import static com.jogamp.opengl.GL2.*;

/*
 This example draws several spheres with different light and material
 attributes.
 Adapted from http://nehe.gamedev.net/
 */
public class RefectiveSpheres extends com.eco.bio7.compile.Model {

	public float rot = 0.0f;
	public double r = 0.0;
	public double g = 0.0;
	public double b = 0.0;
	public float[] no_mat = { 0.0f, 0.0f, 0.0f, 1.0f };
	float[] mat_ambient = { 0.7f, 0.7f, 0.7f, 1.0f };
	float[] mat_ambient_color = { 0.8f, 0.8f, 0.2f, 1.0f };
	float[] mat_diffuse = { 0.1f, 0.5f, 0.8f, 1.0f };
	float[] mat_specular = { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] no_shininess = { 0.0f };
	float[] low_shininess = { 5.0f };
	float[] high_shininess = { 100.0f };
	float[] mat_emission = { 0.3f, 0.2f, 0.2f, 0.0f };

	public DoubleMersenneTwister twist = new DoubleMersenneTwister(new Date());
	public DoubleUniform uni = new DoubleUniform(0.0, 1.0, twist);

	public void run(GL2 gl, GLU glu, GLUT glut) {
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glLightModeli(GL_LIGHT_MODEL_COLOR_CONTROL,
				GL_SEPARATE_SPECULAR_COLOR);
		rot = (rot + 0.1f) % 360;// One rotation and starts with 0.0f!
		r = (r + 0.003) % 1;
		setRotationZ(rot);

		gl.glColor4d(r, g, b, 1.0);

		gl.glPushMatrix();
		gl.glTranslatef(-300.75f, 300.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, no_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in first row, second column diffuse and specular
		 * reflection; low shininess; no ambient
		 */
		gl.glPushMatrix();
		gl.glTranslatef(-100.25f, 300.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, low_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in first row, third column diffuse and specular
		 * reflection; high shininess; no ambient
		 */
		gl.glPushMatrix();
		gl.glTranslatef(100.25f, 300.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, high_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in first row, fourth column diffuse reflection; emission;
		 * no ambient or specular reflection
		 */
		gl.glPushMatrix();
		gl.glTranslatef(300.75f, 300.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, no_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, mat_emission, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in second row, first column ambient and diffuse
		 * reflection; no specular
		 */
		gl.glPushMatrix();
		gl.glTranslatef(-300.75f, 0.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, mat_ambient, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, no_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in second row, second column ambient, diffuse and
		 * specular reflection; low shininess
		 */
		gl.glPushMatrix();
		gl.glTranslatef(-100.25f, 0.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, mat_ambient, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, low_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in second row, third column ambient, diffuse and specular
		 * reflection; high shininess
		 */
		gl.glPushMatrix();
		gl.glTranslatef(100.25f, 0.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, mat_ambient, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, high_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in second row, fourth column ambient and diffuse
		 * reflection; emission; no specular
		 */
		gl.glPushMatrix();
		gl.glTranslatef(300.75f, 0.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, mat_ambient, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, no_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, mat_emission, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in third row, first column colored ambient and diffuse
		 * reflection; no specular
		 */
		gl.glPushMatrix();
		gl.glTranslatef(-300.75f, -300.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, mat_ambient_color, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, no_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in third row, second column colored ambient, diffuse and
		 * specular reflection; low shininess
		 */
		gl.glPushMatrix();
		gl.glTranslatef(-100.25f, -300.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, mat_ambient_color, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, low_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in third row, third column colored ambient, diffuse and
		 * specular reflection; high shininess
		 */
		gl.glPushMatrix();
		gl.glTranslatef(100.25f, -300.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, mat_ambient_color, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, high_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, no_mat, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();
		/*
		 * draw sphere in third row, fourth column colored ambient and diffuse
		 * reflection; emission; no specular
		 */
		gl.glPushMatrix();
		gl.glTranslatef(300.75f, -300.0f, 0.0f);
		gl.glMaterialfv(GL_FRONT, GL_AMBIENT, mat_ambient_color, 0);
		gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, no_mat, 0);
		gl.glMaterialfv(GL_FRONT, GL_SHININESS, no_shininess, 0);
		gl.glMaterialfv(GL_FRONT, GL_EMISSION, mat_emission, 0);
		glut.glutSolidSphere(100.0f, 20, 20);
		gl.glPopMatrix();

	}
}