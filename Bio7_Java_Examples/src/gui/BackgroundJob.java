package gui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/*
 * Runs work of a GUI (R, Python, GDAL) in a job of Bio7, so that the GUI stays
 * responsive, and passes the result (a status text) or the error to the GUI in
 * the UI thread. Used by the GUI examples (python.gui, r.gui, gdal.gui,
 * python.templatematching).
 */
public class BackgroundJob {

	public static void run(String name, Callable<String> task, Consumer<String> done) {
		Job job = new Job(name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String status;
				try {
					status = task.call();
				} catch (Throwable e) {
					/* The last line of the message (a Python traceback or an R error is long) */
					String m = String.valueOf(e.getMessage() != null ? e.getMessage() : e).strip();
					status = "Error: " + m.substring(m.lastIndexOf('\n') + 1);
					e.printStackTrace();
				}
				String text = status;
				Display.getDefault().asyncExec(() -> done.accept(text));
				return Status.OK_STATUS;
			}
		};
		job.setUser(false);
		job.schedule();
	}
}
