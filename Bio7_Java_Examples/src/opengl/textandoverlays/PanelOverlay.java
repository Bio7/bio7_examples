package opengl.textandoverlays;

import java.awt.Graphics2D;
import java.io.*;
import java.awt.*;
import javax.imageio.ImageIO;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import static com.eco.bio7.spatial.SpatialEvents.*;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.gl2.GLUT;

/*
 This example demonstrates the use of an overlay with the Jogl API!
 A double-click with the mouse device makes the overlay panel visible!
 */

public class PanelOverlay extends com.eco.bio7.compile.Model {

	public Overlay infoOverlay = SpatialUtil.createOverlay();
	/* We create to alpha values for demonstration purposes! */
	public AlphaComposite ac1 = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, 0.5f);
	public AlphaComposite ac2 = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, 1.0f);

	public int height = 100;
	public int width = 200;
	public int counter = 0;
	public boolean clicked = false;
	public Font font = new Font("SansSerif", Font.BOLD, 14);
	public String fileroot;
	public String f;
	public Image image;

	public void run(GL2 gl, GLU glu, GLUT glut) {

		if (isMouseDoubleClicked()) {
			clicked = !clicked;
			counter = 0;
		}
		if (clicked) {
			if (SpatialUtil.isSplitView()) {
				/*
				 * The following method avoids actions if the split-screen is
				 * active! The overlay panel is not drawn in the small
				 * split-screen panel. If true the overlay is drawn in the small
				 * panel!
				 */
				if (SpatialUtil.isSplitPanelDrawing() == false) {
					if (infoOverlay != null) {
						showOverlayInfo(gl);
						counter++;
					}
				}
			} else {
				if (infoOverlay != null) {
					showOverlayInfo(gl);
					counter++;
				}

			}
		}

	}

	private void showOverlayInfo(GL2 gl) {

		Graphics2D g2d = infoOverlay.createGraphics();
		g2d.setComposite(ac1);
		g2d.setColor(new Color(0, 0, 0));
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(Color.WHITE);
		g2d.drawLine(0, 0, width, height);
		/* Create the path from the root of the workspace! */
		f = FileRoot.getCurrentCompileDir() + "/opengl/textandoverlays/tree.gif";

		g2d.setComposite(ac2);

		/* Load the image from a file system location! */
		if (image == null) {
			try {
				image = ImageIO.read(new File(f));
			} catch (IOException e1) {

				e1.printStackTrace();
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

		/*
		 * Mark only the needed region for a repaint dirty (counter) for
		 * efficiency!!!!!!
		 */
		infoOverlay.markDirty(0, 0, 200, 50);
		infoOverlay.drawAll();
		g2d.dispose();
	}
}