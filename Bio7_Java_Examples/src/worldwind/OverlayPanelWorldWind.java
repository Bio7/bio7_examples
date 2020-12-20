package worldwind;

import gov.nasa.worldwind.BasicSceneController;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.worldwind.Ww;
import com.jogamp.opengl.util.awt.Overlay;
import com.jogamp.opengl.util.gl2.GLUT;
/*
 This example demonstrates a panel overlay on top of the WorldWind view!
 Add a dynamic layer to see the result!
 The WorldWind perspective has to be active in Bio7 1.4!
 */


public class OverlayPanelWorldWind extends com.eco.bio7.compile.Model {

	/* The WorldWind perspective has to be active! */

	public Object sc = Ww.getWwd().getSceneController();
	public BasicSceneController bsc = (BasicSceneController) sc;
	public DrawContext dc = bsc.getDrawContext();

	public Overlay infoOverlay = new Overlay(dc.getGLDrawable());
	/* We create to alpha values for demonstration purposes! */
	public AlphaComposite ac1 = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, 0.5f);
	public AlphaComposite ac2 = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, 1.0f);

	public int height = 100;
	public int width = 200;
	public int counter = 0;
	public Font font = new Font("SansSerif", Font.BOLD, 14);
	public String fileroot;
	public String f;
	public Image image;

	public void setup(GL2 gl, GLU glu, GLUT glut) {

	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (sc != null) {
			showOverlayInfo(gl);
		}

	}

	private void showOverlayInfo(GL2 gl) {

		Graphics2D g2d = infoOverlay.createGraphics();
		g2d.setComposite(ac1);
		g2d.setColor(new Color(0, 0, 0));
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(Color.WHITE);
		g2d.drawLine(0, 0, width, height);
		fileroot = FileRoot.getFileRoot();
		/* Create the path from the root of the workspace! */
		f = FileRoot.getCurrentCompileDir() + "/opengl/textureandmodels/tree.gif";
		g2d.setComposite(ac2);

		/* Load the image from a file system location! */
		if (image == null) {
			try {
				image = ImageIO.read(new File(f));
			} catch (IOException e1) {

				System.out.println("Can't load file!");
			}
		}
		/* Draw an image! */
		g2d.drawImage(image, 0, 100, null);

		/*
		 * Draw the information with the graphics context!
		 */
		g2d.setColor(Color.WHITE);
		g2d.setFont(font);
		g2d.drawString("Frames counted: " + counter, 5, 15);
		g2d.drawString("Bio7", 5, 33);
		counter++;
		/*
		 * Mark only the needed region for a repaint dirty (counter) for
		 * efficiency!!!!!!
		 */
		infoOverlay.markDirty(0, 0, 200, 50);
		infoOverlay.drawAll();
		g2d.dispose();
		Ww.getWwd().redraw();
	}
}