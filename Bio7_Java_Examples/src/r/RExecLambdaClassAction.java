package r;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.ExecuteRScript;

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
		new ExecuteRScript((RConnection con, IProgressMonitor monitor) -> {
			try {con.eval(rCommand);} 
			catch (RserveException e) {e.printStackTrace();}
				monitor.worked(1);			
		}, 1);

	}
    
	public static void main(String[] args) {
		
	}

}
