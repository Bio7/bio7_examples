package opengl.textandoverlays;

import java.awt.Font;
import java.util.Date;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import static com.eco.bio7.spatial.SpatialUtil.*;
import cern.jet.random.tdouble.DoubleUniform;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;

/*
 This example draws text in 3d!

 !!!!Important!!!!!
 If you use the fullscreen view the creation of the TextRenderer
 must be triggered by means of the setup method to avoid a crash
 of the application. See the manual for more information why.
 */
public class QuadsPoints extends com.eco.bio7.compile.Model {

	public Font font = new Font("Verdana", Font.BOLD, 100);
	public DoubleMersenneTwister twist = new DoubleMersenneTwister(new Date());
	public DoubleUniform uni = new DoubleUniform(-10000.0, 10000.0, twist);
	public double[] x = new double[1000];
	public double[] y = new double[1000];
	public double[] z = new double[1000];
	public double r = 0;
	public TextRenderer renderer;

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		font = new Font("Verdana", Font.BOLD, 100);
		/*
		 * When using the fullscreen option the TextRenderer has to
		 * reinitialized in the setup method again!!!!
		 */
		renderer = new TextRenderer(font, true, false);

	}

	public void run(GL2 gl, GLU glu, GLUT glut) {

		if (renderer == null) {

			font = new Font("Verdana", Font.BOLD, 100);

			renderer = new TextRenderer(font, true, false);
		}
		renderer.begin3DRendering();
		for (int i = 0; i < 1000; i++) {
			/* We randomize the coordinates at each step! */
			if (canStep()) {
				x[i] = uni.nextDouble();
				y[i] = uni.nextDouble();
				z[i] = uni.nextDouble();

			}
			renderer.setColor(0, 0, 0, 1);
			/* Draw the text at the coordinates! */
			renderer.draw3D("Bio7", (float) x[i], (float) y[i], (float) z[i],
					1.0f);

		}
		renderer.end3DRendering();

	}
}