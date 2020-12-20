package r;

import com.eco.bio7.image.ImageMethods;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

/*
 This example calls a RScript which produces a raw vector and transfers
 it to ImageJ as an greyscale image. One image works as a container.
 Please start the R server!
 */
public class createRandomImages2 extends com.eco.bio7.compile.Model {

	public void run() {
		if (RServe.isAlive()) {
			if (RState.isBusy() == false) {
				/*
				 * Get the relative path to the script! Call the RScript without
				 * an output! For debugging call RServe.callRScript(..);
				 */
				RServe.callRScriptSilent("/Bio7_Java_Examples/src/r/randomScript.R");

				/*
				 * Create the byte image directly in an ImageJ image and update
				 * this image with the new values!
				 */
				/*
				 * Special method which transfers raws ...("imageMatrix",1) or
				 * floats ...("imageMatrix",2)
				 */
				ImageMethods.transferImageInPlace("imageMatrix", 1);

			} else {
				System.out.println("RServer is busy!");
			}
		} else {
			System.out.println("RServer is not alive!");
		}
	}
}