package r;
import org.eclipse.core.runtime.IProgressMonitor;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.rbridge.ExecuteRScript;
import com.eco.bio7.rbridge.RScriptExecuter;
import com.eco.bio7.rffm.RException;

/*An example to execute R code running in a job using Raw String Literals with Java 15!*/

public class RExecAbstractClassAction {
	String rCommand = """
			print(runif(100))
			""";
	public RExecAbstractClassAction() {
		/*
		 * The ExecuteRScript class executes the 'evaluate' method of a given RScriptExecuterInterface!
		 * The abstract RScriptExecuter class implements the interface so we can override
		 * the evaluate method of that class which is called from the ExecuteRScript class!
		 */
		new ExecuteRScript(new RScriptExecuter() {
			/* The connection of Bio7: embedded R */
			public void evaluate(RBackend con, IProgressMonitor monitor) {
				try {			
					con.eval(rCommand);
					monitor.worked(1);

				} catch (RException e) {
					e.printStackTrace();
				}
			}

		},1);

	}
    
	public static void main(String[] args) {
		
	}

}
