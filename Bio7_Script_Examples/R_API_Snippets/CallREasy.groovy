//An easy example get and set values in R with the Bio7 API!
import static com.eco.bio7.rbridge.REval.*;
import com.eco.bio7.rffm.RValue;




for (int i = 1; i < 5; i++) {
	/*Evaluate R code or a script (second option)!*/
	evalR("plot(runif(1000)); ", null);
	/*Send values to R!*/
	toR("count",""+i);
	evalR("print(paste('Plot finished Nr.',count,sep=''))", null);
	/*Get values from R!*/
	RValue rValue=fromR("count");
	System.out.println("The last count was Nr."+rValue.firstString());
	
}


