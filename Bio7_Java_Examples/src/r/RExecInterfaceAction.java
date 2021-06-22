package r;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.ExecuteRScript;
import com.eco.bio7.rbridge.RScriptExecuterInterface;

/*An example to execute R code running in a job using Raw String Literals with Java 15!*/
public class RExecInterfaceAction implements RScriptExecuterInterface {
	String rCommand = """
			try(plot(runif(100)))
			""";

	public RExecInterfaceAction() {
		/*
		 * We create a new instance of the ExecuteRScript class which executes the
		 * evaluate method of this given RScriptExecuterInterface interface!
		 */
		new ExecuteRScript(this, 1);
	}
	/* We declare the interface method! */
	public void evaluate(RConnection con, IProgressMonitor monitor) {

		try {
			con.eval(rCommand);
			monitor.worked(1);
		} catch (RserveException e) {
			e.printStackTrace();
		}

	}
	public static void main(String[] args) {
		// Main method to avoid warning!
	}

}
