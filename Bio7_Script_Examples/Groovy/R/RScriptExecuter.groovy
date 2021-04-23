/*An example to execute R code interactive with SWT dialogs in a job and instant plotting support!*/
import org.eclipse.core.runtime.IProgressMonitor;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.ExecuteRScript;
import com.eco.bio7.rbridge.RScriptExecuter;
import com.eco.bio7.rbridge.RScriptExecuterInterface;

loopPlotCommand = '''
	renderPlot <- function() {
		    for (x in (1:10)) {
		        par(mfrow = c(2, 3))
		        boxplot(runif(1000))
		        plot(runif(1000))
		        image(matrix(runif(100) * 2500, 10, 10), useRaster = TRUE)
		        hist(runif(100))
		        pie(runif(10), main = x)
		        mat <- matrix(runif(100) * 2500, 10, 10)
		        # contour(mat,main='Plot');
		        persp(mat, main = "Plot", theta = 30, phi = 30, expand = 0.5, col = "red")
		    }
		}
		try(renderPlot());
		
						''';
individualLoopPlotCommand = '''
	renderPlot <- function() {	    
		        par(mfrow = c(2, 3))
		        boxplot(runif(1000))
		        plot(runif(1000))
		        image(matrix(runif(100) * 2500, 10, 10), useRaster = TRUE)
		        hist(runif(100))
		        pie(runif(10), main = 1)
		        mat <- matrix(runif(100) * 2500, 10, 10)
		        # contour(mat,main='Plot');
		        persp(mat, main = "Plot", theta = 30, phi = 30, expand = 0.5, col = "red")		    
		}
		try(renderPlot());
		
						''';
scanCommand = """
	x<-scan();
	""";
boxplotCommand = """
	boxplot(x);
	""";
histCommand = """
	hist(x);
	""";
runifCommand = """
	print(runif(100))
	""";
/*Interface to Rserve running in a job with progress indication!*/
new ExecuteRScript(new RScriptExecuter() {
	public void evaluate(RConnection con, IProgressMonitor monitor) {
		decision1 = Bio7Dialog.decision("Print random data (runif)?");
		if (decision1) {
			con.eval(runifCommand);
		}
		/*Visualizing of progress in the Bio7 job!*/
		monitor.worked(1);

		decision2 = Bio7Dialog.decision("Enter data and plot data?");
		if (decision2) {
			
			Bio7Action.clearConsole();
			Bio7Dialog.message("Please enter the data in the Bio7 console or R-Shell!");
			con.eval(scanCommand);
			con.eval(boxplotCommand);
			/*Display the plot instantly instead to plot all at the end!*/
			RServeUtil.displayPlotEvent();
			decision3 = Bio7Dialog.decision("Plot data as histogram, too?");
			if (decision3) {
				con.eval(histCommand);
				RServeUtil.displayPlotEvent();

			}
		}
		monitor.worked(1);
		decision4 = Bio7Dialog.decision("Plot an loop example as a stack?");
		if (decision4) {
			con.eval(loopPlotCommand);
			RServeUtil.displayPlotEvent();

		}
		monitor.worked(1);
		decision5 = Bio7Dialog.decision("Plot an loop example as individual plots?");
		if (decision5) {
			for (int i = 0; i < 5; i++) {
				con.eval(individualLoopPlotCommand);
				RServeUtil.displayPlotEvent();
			}
		}
		monitor.worked(1);
	}
}, 4);
