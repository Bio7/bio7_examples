package modelling.random;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.database.Bio7State;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.rbridge.RServe;

/*
 This Snippet produces random numbers in R to transfer them 
 to the discrete Field in Bio7.

 Please start the Rserver to get a connection to R!
 Enable some states (Plants) to see the result!
 */
public class FromRSim extends com.eco.bio7.compile.Model {

	double[] y;
	double[] x;
	String fieldsize;

	public void run() {
		fieldsize = Integer.toString(Field.getHeight() * Field.getWidth());
		int i = 0;
		int u = 0;
		try {
			RServe.getConnection().eval(
					"y<-runif(" + fieldsize + ",0," + Bio7State.getStateSize()
							+ ")");
			try {
				y = RServe.getConnection().eval("y").asDoubles();

			} catch (REXPMismatchException e1) {

				e1.printStackTrace();
			}

		} catch (RserveException e) {

			System.out.println(e.getRequestErrorDescription());
		}
		/* We use only one loop to assign the values! */
		for (int z = 0; z < (Field.getHeight() * Field.getWidth()); z++) {

			if (u > (Field.getWidth() - 1)) {
				i++;
				u = 0;
			}
			/*
			 * Here we assign the values. The method Math.abs(..) returns the
			 * absolute value!
			 */
			Field.setState(u, i, (int) (Math.abs(y[z])));

			if (u < Field.getWidth()) {
				u++;
			}
		}
	}
}
