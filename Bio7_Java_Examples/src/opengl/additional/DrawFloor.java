package opengl.additional;

import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL2GL3.GL_QUADS;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

/*
Example from the book "Pro Java 6 3D Game Development" from Andrew Davison to draw
a Checkerboard with text.

Important: 
If you use the fullscreen view the creation of the  TextRenderer
must be triggered by means of the setup method to avoid a crash
of the application. See the manual for more information why.
*/
public class DrawFloor extends com.eco.bio7.compile.Model {
	

private final static int FLOOR_LEN = 200; // should be even
private final static int BLUE_TILE = 0; // floor tile colour types
private final static int GREEN_TILE = 1;
private final static float SCALE_FACTOR = 0.01f; // for the axis labels
  
public Font font = new Font("SansSerif", Font.BOLD, 24);
   //FontMetrics  metrics = this.getFontMetrics(font);
public TextRenderer axisLabelRenderer = new TextRenderer(font);
  
public void setup(GL2 gl,GLU glu,GLUT glut){
    /*
    This will create a new object with the correct GL context
    if using the fullscreen mode.
    */
    axisLabelRenderer = new TextRenderer(font);
  
}

public void run(GL2 gl, GLU glu, GLUT glut) {
	gl.glEnable(GL_DEPTH_TEST);
	//gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	gl.glScaled(100, 100, 100);
	gl.glRotated(90.0, 1.0, 0, 0);
	drawFloor(gl);
}

private void drawFloor(GL2 gl)
  /* Create tiles, the origin marker, then the axes labels.
     The tiles are in a checkboard pattern, alternating between
     green and blue.
  */
  {
    gl.glDisable(GL_LIGHTING);

    drawTiles(BLUE_TILE,gl);   // blue tiles
    drawTiles(GREEN_TILE,gl);  // green
    addOriginMarker(gl);
    labelAxes();

    gl.glEnable(GL_LIGHTING);
  }  // end of CheckerFloor()


  private void drawTiles(int drawType,GL2 gl)
  /* Create a series of quads, all with the same colour. They are
     spaced out over a FLOOR_LEN*FLOOR_LEN area, with the area centered
     at (0,0) on the XZ plane, and y==0.
  */
  {
    if (drawType == BLUE_TILE)
      gl.glColor3f(0.0f, 0.1f, 0.4f);
    else  // green
      gl.glColor3f(0.0f, 0.5f, 0.1f);

    gl.glBegin(GL_QUADS);
    boolean aBlueTile;
    for(int z=-FLOOR_LEN/2; z <= (FLOOR_LEN/2)-1; z++) {
      aBlueTile = (z%2 == 0)? true : false;    // set colour type for new row
      for(int x=-FLOOR_LEN/2; x <= (FLOOR_LEN/2)-1; x++) {
        if (aBlueTile && (drawType == BLUE_TILE))  // blue tile and drawing blue
          drawTile(x, z,gl);
        else if (!aBlueTile && (drawType == GREEN_TILE))   // green
          drawTile(x, z,gl);
        aBlueTile = !aBlueTile;
      }
    }
    gl.glEnd();
  }  // end of drawTiles()


  private void drawTile(int x, int z,GL2 gl)
  /* Coords for a single blue or green square; 
    its top left hand corner at (x,0,z). */
  {
    // points created in counter-clockwise order
    gl.glVertex3f(x, 0.0f, z+1.0f);   // bottom left point
    gl.glVertex3f(x+1.0f, 0.0f, z+1.0f);
    gl.glVertex3f(x+1.0f, 0.0f, z);
    gl.glVertex3f(x, 0.0f, z);
  }  // end of drawTile()


  private void addOriginMarker(GL2 gl)
  /* A red square centered at (0,0.01,0), of length 0.5, lieing
     flat on the XZ plane. */
  {  
    gl.glColor3f(0.8f, 0.4f, 0.3f);   // medium red
    gl.glBegin(GL_QUADS);

    // points created counter-clockwise, a bit above the floor
    gl.glVertex3f(-0.25f, 0.01f, 0.25f);  // bottom left point
    gl.glVertex3f(0.25f, 0.01f, 0.25f);
    gl.glVertex3f(0.25f, 0.01f, -0.25f);    
    gl.glVertex3f(-0.25f, 0.01f, -0.25f);

    gl.glEnd();
  } // end of addOriginMarker();


  private void labelAxes()
  // Place numbers along the x- and z-axes at the integer positions
  {
  	axisLabelRenderer.begin3DRendering();
    for (int i=-FLOOR_LEN/2; i <= FLOOR_LEN/2; i++)
      drawAxisText(""+i, (float)i, 0.0f, 0.0f);  // along x-axis

    for (int i=-FLOOR_LEN/2; i <= FLOOR_LEN/2; i++)
      drawAxisText(""+i, 0.0f, 0.0f, (float)i);  // along z-axis
      
       axisLabelRenderer.end3DRendering();
  }  // end of labelAxes()


  private void drawAxisText(String txt, float x, float y, float z) 
  /* Draw txt at (x,y,z), with the text centered in the x-direction,
     facing along the +z axis.
  */
  {
    Rectangle2D dim = axisLabelRenderer.getBounds(txt);
    float width = (float)dim.getWidth() * SCALE_FACTOR;

    
    axisLabelRenderer.draw3D(txt, x-width/2, y, z, SCALE_FACTOR);
   
  } // end of drawAxisText()
}

 