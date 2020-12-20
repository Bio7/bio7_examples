package worldwind;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.worldwind.WorldWindView;
import com.eco.bio7.worldwind.swt.WorldWindowNewtCanvasSWT;

import gov.nasa.worldwind.render.DrawContext;
import cern.jet.random.tdouble.DoubleUniform;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;
import java.util.Date;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.worldwind.*;
import com.jogamp.opengl.util.gl2.GLUT;
import static com.jogamp.opengl.GL2.*;

/*
 This example demonstrates how to draw lighted spheres
 dynamically and georeferenced!
 Open the WorldWind perspective compile this file and enable a dynamic layer.
 Fly to the Brocken in Germany (N 51.7763,E 10.6069) and adjust the elevation!
 The WorldWind perspective has to be active in Bio7 1.4!
 */
public class DrawSpheresDynamically extends com.eco.bio7.compile.Model {

	public float[] fBrightLight = { 1.0f, 1.0f, 1.0f, 1.0f };
	private Material junctionMaterial = Material.RED;
	/* The WorldWind perspective has to be active! */
	public Object sc;
	public BasicSceneController bsc;
	public DrawContext dc;

	public DoubleMersenneTwister twist = new DoubleMersenneTwister(new Date());
	public DoubleUniform uni = new DoubleUniform(-0.02f, 0.02f, twist);

	public double lat = 51.7991;
	public double lon = 10.6158;
	public double elevation = 1141;

	public DrawSpheresDynamically() {

		WorldWindowNewtCanvasSWT wwc = WorldWindView.getWwd();
		if (wwc != null) {
			sc = wwc.getSceneController();
			bsc = (BasicSceneController) sc;
			dc = bsc.getDrawContext();
		} else {
			Bio7Dialog.message("Please open the WorldWind perspective!");
		}

	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {

	}

	public void run(GL2 gl, GLU glu, GLUT glut) {

		drawShape(gl, glu, glut);
	}

	public void drawShape(GL2 gl, GLU glu, GLUT glut) {

		if (DynamicLayer.canStep()) {
			/*
			 * The step method can be used to control the speed and can be
			 * adjusted with the slider of the dynamic layer!
			 */
		}
		/*
		 * This is the way how we draw the points on the correct coordinates in
		 * WorldWind!
		 */
		DynamicLayer.getDynamicLayer().begin(dc);

		for (int i = 0; i < 80; i++) {
			/* We create random coordinates in a certain range! */
			lat = lat + uni.nextDouble();
			lon = lon + uni.nextDouble();
			/* Calculate the elevations and positions! */
			elevation = Ww
					.getWwd()
					.getModel()
					.getGlobe()
					.getElevation(Angle.fromDegrees(lat),
							Angle.fromDegrees(lon));
			Position position = new Position(Angle.fromDegrees(lat),
					Angle.fromDegrees(lon), elevation);
			Vec4 pos = dc.getGlobe().computePointFromPosition(position);

			dc.getView().pushReferenceCenter(dc, pos);
			/*
			 * Here we draw the spheres with material attributes and with red
			 * colour!
			 */
			gl.glColor4d(0.5, 0.1, 0.1, 1.0);
			gl.glEnable(GL_COLOR_MATERIAL);
			gl.glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
			gl.glMaterialfv(GL_FRONT, GL_SPECULAR, fBrightLight, 0);
			gl.glMateriali(GL_FRONT, GL_SHININESS, 110);
			glut.glutSolidSphere(100, 20, 20);
			lat = 51.7991;
			lon = 10.6158;
			dc.getView().popReferenceCenter(dc);
		}

		DynamicLayer.getDynamicLayer().end(dc);
		// Restore OpenGL variables.

		Ww.getWwd().redraw();
	}
}