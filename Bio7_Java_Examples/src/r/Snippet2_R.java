package r;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RServe;

/* 
This Snippet is an easy example how to get variables
from R after a calculation and embedd it in an 
simulation run (try..catch.. are necessary)!
*/
public class Snippet2_R extends com.eco.bio7.compile.Model {

public void run() {
	double[] result = null;
	if (RServe.isAlive()) {
		try {
			RServe.getConnection().eval("A<-c(runif(100)*100)");
			try {
				//get the result from R
				result = (double[]) RServe.getConnection().eval("summary(A)")
					.asDoubles();

			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (RserveException e) {

			System.out.println(e.getRequestErrorDescription());
		}

		for (int i = 0; i < result.length; i++) {
			System.out.println(result[i] + " ");
		}
	} else {
		System.out.println("No connection to Rserve available!");
	}
}}