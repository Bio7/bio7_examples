/*
An easy example: send values to R, evaluate R code and get the results back
with RUtil, the API of the embedded R for Java, Groovy and BeanShell.
The script runs in a job of Bio7, thus the R calls don't block Bio7.
*/
import com.eco.bio7.rbridge.RUtil

if (!RUtil.isAlive()) {
	Bio7Dialog.message("R is not running! Please start R first.")
	return
}
for (int i = 1; i < 5; i++) {
	/* Send a value to R */
	RUtil.toR("count", i)
	/* Evaluate R code */
	RUtil.exec("x <- runif(1000) * count")
	/* Get the results from R */
	double mean = RUtil.fromRDouble("mean(x)")
	String text = RUtil.fromRString("paste('Run Nr.', count)")
	println(text + ": the mean is " + mean)
}
