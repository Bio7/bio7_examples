package r;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.database.Bio7State;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.rbridge.RServe;

/*
 This Snippet produces random numbers in R to transfer them 
 to the discrete Quadgrid field in Bio7.
 Please start the R server to produce the random numbers!
 */
public class Snippet1_FromRSim extends com.eco.bio7.compile.Model {

	double[] y;
	double[] x;
	String fieldsize;

	public void run() {
		fieldsize = Integer.toString(Field.getHeight() * Field.getWidth());
		int i = 0;
		int u = 0;
		if (RServe.isAlive()) {
			try {
				RServe.getConnection().eval(
						"y<-runif(" + fieldsize + ",0,"
								+ Bio7State.getStateSize() + ")");
				// RServe.getConnection().eval("x<-runif(2500,0,2)");
				try {
					y = RServe.getConnection().eval("y").asDoubles();
					// x=RServe.getConnection().eval("x").asDoubleArray();
				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (RserveException e) {

				System.out.println(e.getRequestErrorDescription());
			}

			for (int z = 0; z < Field.getHeight() * Field.getWidth(); z++) {

				if (u > (Field.getWidth() - 1)) {
					i++;
					u = 0;
				}
				Field.setState(u, i, (int) (Math.abs(y[z])));

				if (u < Field.getWidth()) {
					u++;
				}
			}
		} else {
			System.out.println("No connection to Rserve available!");
		}
	}
}
