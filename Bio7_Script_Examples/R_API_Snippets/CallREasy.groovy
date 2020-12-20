//An easy example get and set values in R with the Bio7 API!
import static com.eco.bio7.rbridge.RServeUtil.*;
import org.rosuda.REngine.REXP;




for (int i = 1; i < 5; i++) {
	/*Evaluate R code or a script (second option)!*/
	evalR("plot(runif(1000)); ", null);
	/*Send values to R!*/
	toR("count",""+i);
	evalR("print(paste('Plot finished Nr.',count,sep=''))", null);
	/*Get values from R!*/
	REXP rexpr=fromR("count");
	System.out.println("The last count was Nr."+rexpr.asString());
	
}


