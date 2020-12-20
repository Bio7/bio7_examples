package extra;

import ij.io.Opener;
import javax.swing.SwingUtilities;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.rbridge.RServe;

/*
 This example shows how you can create(rendered)graphs with the
 R application (Old example - you can now use the general plot function or the special Bio7Rserve API!).

 For this example the Rserve application has to be started!

 This example creates R plots. This plots then are opened
 in the ImageJ view (which then can be converted to a stack
 and saved as an avi file!).
 The default path is the path to the project!
 Only one image is created. In this image the different plots are rendered
 and then opened in the ImageJ view.

 You can change this example to use for a custom dynamic plot
 Tested with 1500 histogramm plots (400,400)!
 Only the heap is the limit (watch the heap with the garbage can icon in the status bar!)
 The heap can be changed in the bio7.ini file (in the folder where Bio7.exe exist!)
 with an (text)editor for more memory!
 See also: http://www.eclipsezone.com/eclipse/forums/t61618.html
 */

public class RenderPlotToImageJ extends com.eco.bio7.compile.Model {

	String directory = null;
	String f = null;
	String root = null;

	public void setup() {

	}

	public void run() {

		if (RServe.isAlive()) {
			/* The root of the workspace */
			//root = FileRoot.getFileRoot();
			/* Get the path and convert it for R (Windows) */
			f = FileRoot.getCurrentCompileDir() + "/extra/plot.png";

			/* For Windows we have to replace "/" to "\\" ! */
			//f = f.replace("/", "\\");

			/* Transfer path to R ! */

			try {
				RServe.getConnection().assign("fileroot", f);
				/*
				 * We create a *.png file in the project. Use the Refresh action
				 * to see the image!
				 */
				RServe.getConnection().eval(
						"try(png(fileroot,width=500,height=500))");

				/*-----------Here you can place your custom plot!---------------------------*/

				RServe.getConnection().eval("x<-runif(1:1000)");
				RServe.getConnection().eval("hist(x);");

				/*------------------------------------------------------------------------*/

				/* We close the device! */
				RServe.getConnection().eval("dev.off();");

			}

			catch (RserveException e) {

				System.out.println(e.getRequestErrorDescription());
			}
			/* Now we open the plot in ImageJ as a tab! */
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Opener o = new Opener();
					o.open(f);
				}
			});
		}

		else {

			Bio7Action.stopCalculation();
			Bio7Dialog.message("Rserve application is not alive!");
			System.out.println("Rserve is not alive!");

		}

	}
}