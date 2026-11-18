package r;

import org.eclipse.core.runtime.IProgressMonitor;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.rbridge.ExecuteRScript;
import com.eco.bio7.rbridge.RScriptExecuterInterface;
import com.eco.bio7.rffm.RException;

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
	/* We declare the interface method with the connection of Bio7 to R! */
	public void evaluate(RBackend con, IProgressMonitor monitor) {

		try {
			con.eval(rCommand);
			monitor.worked(1);
		} catch (RException e) {
			e.printStackTrace();
		}

	}
	public static void main(String[] args) {
		// Main method to avoid warning!
	}

}
