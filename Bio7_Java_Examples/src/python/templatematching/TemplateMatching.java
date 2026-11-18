package python.templatematching;

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.pybridge.PythonUtil;
import com.eco.bio7.pyffm.PyValue;

import gui.BackgroundJob;
import python.PythonExamples;

/*
 * A Java GUI (SWT, in a view of Bio7) for the Python functions of the
 * template matching with OpenCV (Bio7_Script_Examples/Python/OpenCV/
 * template_matching.py): a test image, templates from ImageJ selections, the
 * detection with method, threshold and multi-scale, the matches in a table and
 * selected in ImageJ.
 *
 * The GUI is in TemplateMatchingGui (open it with the WindowBuilder: right
 * click > Open With > WindowBuilder Editor). This class (the model) calls
 * Python in a job (the GUI stays responsive) and updates the GUI.
 *
 * Requires: the embedded Python with opencv-python-headless (Python Packages:
 * the bundle "OpenCV"). Python is started if necessary.
 */
public class TemplateMatching extends com.eco.bio7.compile.Model {

	private TemplateMatchingGui gui;
	private boolean imported;

	public TemplateMatching() {
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Template Matching");
			parent.setLayout(new FillLayout());
			gui = new TemplateMatchingGui(parent, TemplateMatching.this, SWT.NONE);
			parent.layout(true);
		});
	}

	/* The module template_matching imported once (its templates are kept) */
	private void python() throws Exception {
		if (!imported) {
			PythonUtil.exec(PythonExamples.addToPath(PythonExamples.folder("OpenCV")) + "import template_matching as tm\n");
			imported = true;
		}
	}

	public void createTestImage() {
		job("Test image", () -> {
			python();
			PythonUtil.exec("_tm_n = tm.test_image()");
			updateTemplates();
			return "Test image with " + (int) ((PyValue.Array) PythonUtil.fromPython("_tm_n")).asDouble() + " shapes, a template of 4 shapes";
		});
	}

	public void addTemplate(String name) {
		if (name == null || name.isBlank()) {
			gui.setStatus("Please enter a name for the template");
			return;
		}
		job("Template", () -> {
			python();
			PythonUtil.exec("tm.add_template(" + PythonExamples.quote(name.strip()) + ")");
			updateTemplates();
			return "Template \"" + name.strip() + "\" added";
		});
	}

	public void clearTemplates() {
		job("Templates", () -> {
			python();
			PythonUtil.exec("tm.clear_templates()");
			updateTemplates();
			return "Templates removed";
		});
	}

	public void detect(String method, double threshold, boolean multiscale) {
		job("Template matching", () -> {
			python();
			PythonUtil.exec(String.format(Locale.ROOT, "_tm_result = tm.run(%.3f, %s, %s)", threshold, PythonExamples.quote(method), multiscale ? "True" : "False"));
			List<String> names = PythonExamples.strings(PythonUtil.fromPython("tm.template_names()"));
			PyValue.Array a = (PyValue.Array) PythonUtil.fromPython("_tm_result");
			double[][] rows = a.length() == 0 ? new double[0][] : a.asDoubleMatrix();
			Display.getDefault().asyncExec(() -> gui.setResults(names, rows));
			return rows.length + " matches selected in ImageJ (ROI Manager)";
		});
	}

	private void updateTemplates() throws Exception {
		List<String> names = PythonExamples.strings(PythonUtil.fromPython("tm.template_names()"));
		Display.getDefault().asyncExec(() -> gui.setTemplates(names));
	}

	/* Python in a job: the result or the error in the status line of the GUI */
	private interface Task {
		String run() throws Exception;
	}

	private void job(String name, Task task) {
		gui.setBusy(true);
		BackgroundJob.run(name, task::run, text -> {
			if (!gui.isDisposed()) {
				gui.setStatus(text);
				gui.setBusy(false);
			}
		});
	}
}
