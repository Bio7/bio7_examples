package r;

/*An example to transfer Particle measurements dynamically!*/
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.rbridge.RBridge;
import javax.swing.SwingUtilities;

import ij.WindowManager;

import com.eco.bio7.image.r.IJTranserResultsTable;
import com.eco.bio7.rffm.RException;

public class ParticleAnalysis extends com.eco.bio7.compile.Model {
	int count = 0;

	public void setup() {
		/*SWT GUI action needs to be on the Swing thread!*/
		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				count = 0;
				analyze();
			}
		});

	}

	public void run() {
		count++;
		analyze();

	}

	private void analyze() {
		if (WindowManager.getCurrentImage() != null) {
			RBackend c = RBridge.getBackend();
			if (c != null) {
				/* Special method to reuse the GUI method! */
				 IJTranserResultsTable.runParticleAnalysis(c, "run(\"Analyze Particles...\", \"size=0-Infinity circularity=0.00-1.00 show=Nothing display clear\")");

				try {
					c.eval("assign(paste0('Particles'," + count + ",sep=''),Particles)");
				} catch (RException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}