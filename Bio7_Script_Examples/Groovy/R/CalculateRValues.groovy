/* 
This Groovy Snippet is an easy example how to get variables
from R after a calculation!
*/
import com.eco.bio7.rbridge.*;
import org.rosuda.REngine.Rserve.RConnection; 
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPDouble;

RConnection c = RServe.getConnection();

if (c!= null) {
	
	c.eval("A<-c(3,5,3,6)");
	c.eval("B<-c(3,6,4,5)");
	c.eval("C<-A+B");
	//get the result from R
	double[] result = (double[])c.eval("summary(A)").asDoubles();

	RServe.print("C"); //easy print function to console

	for (int i = 0; i < result.length; i++) {
		System.out.print(result[i] + " ");
	}
}

else {
	
	Bio7Dialog.message("No Rserve connection available !");
}