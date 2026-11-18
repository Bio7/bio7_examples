package r;

import org.eclipse.core.runtime.IProgressMonitor;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.rbridge.ExecuteRScript;
import com.eco.bio7.rffm.RException;

/*An example to execute R code running in a job using Raw String Literals with Java 15!*/

public class RExecLambdaClassAction {
	String rCommand = """
			print(runif(100))
			""";
	public RExecLambdaClassAction() {
		/*
		 * The ExecuteRScript class executes the 'evaluate' method of a given RScriptExecuterInterface!
		 * The abstract RScriptExecuter class has only one method (evaluate) and implements the RScriptExecuterInterface
		 * interface so we can write it as a Lambda expression (essentially overrides
		 * the evaluate method of the abstract RScriptExecuter class)!
		 */		
		/* ExecuteRScript.run: the lambda gets the connection of Bio7 (embedded R) */
		ExecuteRScript.run((RBackend con, IProgressMonitor monitor) -> {
			try {con.eval(rCommand);} 
			catch (RException e) {e.printStackTrace();}
				monitor.worked(1);			
		}, 1);

	}   
	public static void main(String[] args) {
		
	}

}
