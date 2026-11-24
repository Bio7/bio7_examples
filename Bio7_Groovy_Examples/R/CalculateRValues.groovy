/*
Get values from R after a calculation (RUtil, the API of the embedded R).
*/
import com.eco.bio7.rbridge.RUtil

if (!RUtil.isAlive()) {
	Bio7Dialog.message("R is not running! Please start R first.")
	return
}
RUtil.exec("A <- c(3, 5, 3, 6)")
RUtil.exec("B <- c(3, 6, 4, 5)")
RUtil.exec("C <- A + B")
/* The results as Java values */
double[] summary = RUtil.fromRDoubles("summary(A)")
String[] names = RUtil.fromRStrings("names(summary(A))")
for (int i = 0; i < summary.length; i++) {
	println(names[i] + ": " + summary[i])
}
/* The printed output of R (as in the console) */
println(RUtil.print("C"))
