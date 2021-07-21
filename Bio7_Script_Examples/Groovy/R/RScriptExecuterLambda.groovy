//An example to execute R code in a job!
import org.eclipse.core.runtime.IProgressMonitor;
import com.eco.bio7.rbridge.ExecuteRScript;
/*
A simple R call written as a lambda expression runing in a job!
*/
new ExecuteRScript((rserveCon, monitor) -> {
	rserveCon.eval("print(runif(100))");
	monitor.worked(1);
}, 1);