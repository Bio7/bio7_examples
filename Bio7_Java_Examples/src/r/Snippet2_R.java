package r;

import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

/* 
This Snippet is an easy example how to get variables
from R after a calculation and embedd it in an 
simulation run (try..catch.. are necessary)!
*/
public class Snippet2_R extends com.eco.bio7.compile.Model {

public void run() {
	double[] result = null;
	if (RTools.isAlive()) {
		try {
			RBridge.getBackend().eval("A<-c(runif(100)*100)");
			try {
				//get the result from R
				result = (double[]) RBridge.getBackend().eval("summary(A)")
					.doubles();

			} catch (RTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (RException e) {

			System.out.println(e.getMessage());
		}

		for (int i = 0; i < result.length; i++) {
			System.out.println(result[i] + " ");
		}
	} else {
		System.out.println("R is not running!");
	}
}}