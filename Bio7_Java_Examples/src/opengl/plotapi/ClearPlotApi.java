package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * Removes the plot of the plot API from the 3D view (and its title, axes and
 * legends).
 */
public class ClearPlotApi {

	public static void main(String[] args) {
		Plot3D.clear();
	}
}
