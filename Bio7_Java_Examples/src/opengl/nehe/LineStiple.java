package opengl.nehe;

import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/*
 Draw some lines with attributes!
 Adapted from http://nehe.gamedev.net/
 */
public class LineStiple extends com.eco.bio7.compile.Model {

	public void run(GL2 gl, GLU glu, GLUT glut) {

		gl.glColor3f(1.0f, 1.0f, 1.0f);
		/* in 1st row, 3 lines, each with a different stipple */
		gl.glEnable(GL_LINE_STIPPLE);

		gl.glLineStipple(1, (short) 0x0101); /* dotted */
		drawOneLine(gl, 50.0f, 125.0f, 150.0f, 125.0f);
		gl.glLineStipple(1, (short) 0x00FF); /* dashed */
		drawOneLine(gl, 150.0f, 125.0f, 250.0f, 125.0f);
		gl.glLineStipple(1, (short) 0x1C47); /* dash/dot/dash */
		drawOneLine(gl, 250.0f, 125.0f, 350.0f, 125.0f);

		/* in 2nd row, 3 wide lines, each with different stipple */
		gl.glLineWidth(5.0f);
		gl.glLineStipple(1, (short) 0x0101); /* dotted */
		drawOneLine(gl, 50.0f, 100.0f, 150.0f, 100.f);
		gl.glLineStipple(1, (short) 0x00FF); /* dashed */
		drawOneLine(gl, 150.0f, 100.0f, 250.0f, 100.0f);
		gl.glLineStipple(1, (short) 0x1C47); /* dash/dot/dash */
		drawOneLine(gl, 250.0f, 100.0f, 350.0f, 100.0f);
		gl.glLineWidth(1.0f);

		/* in 3rd row, 6 lines, with dash/dot/dash stipple */
		/* as part of a single connected line strip */
		gl.glLineStipple(1, (short) 0x1C47); /* dash/dot/dash */
		gl.glBegin(GL.GL_LINE_STRIP);
		for (int i = 0; i < 7; i++)
			gl.glVertex2f(50.0f + ((float) i * 50.0f), 75.0f);
		gl.glEnd();

		/* in 4th row, 6 independent lines with same stipple */
		for (int i = 0; i < 6; i++) {
			drawOneLine(gl, 50.0f + ((float) i * 50.0f), 50.0f,
					50.0f + ((float) (i + 1) * 50.0f), 50.0f);
		}

		/* in 5th row, 1 line, with dash/dot/dash stipple */
		/* and a stipple repeat factor of 5 */
		gl.glLineStipple(5, (short) 0x1C47); /* dash/dot/dash */
		drawOneLine(gl, 50.0f, 25.0f, 350.0f, 25.0f);

		gl.glDisable(GL_LINE_STIPPLE);
	}

	private void drawOneLine(GL2 gl, float x1, float y1, float x2, float y2) {
		gl.glBegin(GL.GL_LINES);
		gl.glVertex2f((x1), (y1));
		gl.glVertex2f((x2), (y2));
		gl.glEnd();
	}
}