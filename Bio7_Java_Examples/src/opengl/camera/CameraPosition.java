package opengl.camera;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.spatial.SpatialCamera;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.awt.Overlay;
import com.jogamp.opengl.util.gl2.GLUT;


public class CameraPosition extends com.eco.bio7.compile.Model {
	/*
	 * This example demonstrates the use of the API to get the position of the
	 * camera if the walkthrough option is enabled. An overlay panel shows the
	 * coordinates of the current postion!
	 */

	/* A Bio7 method to create an Overlay instance! */
	public Overlay infoOverlay = SpatialUtil.createOverlay();

	public Font font = new Font("Verdana", Font.BOLD, 12);
	public int height = 100;
	public int width = 200;

	public void run(GL2 gl, GLU glu, GLUT glut) {

		showOverlayInfo(gl);

	}

	public void showOverlayInfo(GL2 gl) {

		Graphics2D g2d = infoOverlay.createGraphics();

		AlphaComposite ac1 = AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, 0.5f);
		g2d.setComposite(ac1);
		g2d.setColor(new Color(0, 0, 0));
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(Color.WHITE);
		g2d.setFont(font);
		g2d.drawString("X: " + SpatialCamera.getXCamPos(), 5, 15);
		g2d.drawString("Y: " + SpatialCamera.getYCamPos(), 5, 30);
		g2d.drawString("Z: " + SpatialCamera.getZCamPos(), 5, 45);

		/* Mark only needed region dirty for efficiency!!!!!! */
		infoOverlay.markDirty(0, 0, 200, 300);
		infoOverlay.drawAll();
		g2d.dispose();
	}
}