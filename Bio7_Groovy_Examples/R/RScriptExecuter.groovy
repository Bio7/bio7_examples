/*
R code with dialogs, progress and plots shown at once (RUtil, the API of the embedded R).

The script runs in a job of Bio7, thus the R calls don't block Bio7 and the dialogs
can be used in between. REval.displayPlotEvent() shows the plots made so far at once
(instead of at the end of the script).
*/
import com.eco.bio7.rbridge.RUtil
import com.eco.bio7.rbridge.REval

if (!RUtil.start()) {
	Bio7Dialog.message("R could not be started (see the console)!")
	return
}
panels = """
	par(mfrow = c(2, 3))
	boxplot(runif(1000))
	plot(runif(1000))
	image(matrix(runif(100) * 2500, 10, 10), useRaster = TRUE)
	hist(runif(100))
	pie(runif(10), main = i)
	persp(matrix(runif(100) * 2500, 10, 10), main = "Plot", theta = 30, phi = 30, expand = 0.5, col = "red")
	"""

if (Bio7Dialog.decision("Print random data (runif)?")) {
	println(RUtil.print("runif(10)"))
}
if (Bio7Dialog.decision("Create data and plot them?")) {
	RUtil.exec("x <- rnorm(200, mean = 10, sd = 2)")
	RUtil.exec("boxplot(x)")
	REval.displayPlotEvent()
	if (Bio7Dialog.decision("Plot the data as histogram, too?")) {
		RUtil.exec("hist(x)")
		REval.displayPlotEvent()
	}
}
if (Bio7Dialog.decision("Plot five pages of plots at once?")) {
	RUtil.exec("for (i in 1:5) { " + panels + " }")
	REval.displayPlotEvent()
}
if (Bio7Dialog.decision("Plot five pages one after the other?")) {
	for (int i = 1; i <= 5; i++) {
		RUtil.toR("i", i)
		RUtil.exec(panels)
		REval.displayPlotEvent()
	}
}
