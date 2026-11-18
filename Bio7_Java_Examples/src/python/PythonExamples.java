package python;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.pyffm.PyValue;

/*
 * Helpers of the Java examples which use Python: the folder of the Python
 * examples (in the project Bio7_Script_Examples of the workspace) and the
 * conversion of Python values.
 */
public class PythonExamples {

	private static final java.util.Map<String, Path> CHOSEN = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * A folder of the Python examples, e.g. folder("OpenCV"): in the project
	 * Bio7_Script_Examples of the workspace, else selected once in a directory
	 * dialog of Bio7.
	 */
	public static Path folder(String name) {
		Path p = Path.of(FileRoot.getWorkspacePath(), "Bio7_Script_Examples", "Python", name);
		if (Files.isDirectory(p)) {
			return p;
		}
		Path chosen = CHOSEN.get(name);
		if (chosen == null) {
			String dir = Bio7Dialog.directory("Select the folder Python/" + name + " of the Bio7 examples");
			if (dir == null) {
				throw new IllegalStateException("The folder Python/" + name + " of the Bio7 examples was not found (project Bio7_Script_Examples)");
			}
			chosen = Path.of(dir);
			CHOSEN.put(name, chosen);
		}
		return chosen;
	}

	/** Python code which makes the modules of a folder importable. */
	public static String addToPath(Path folder) {
		return "import sys\n_p = " + quote(folder.toString()) + "\nif _p not in sys.path:\n    sys.path.insert(0, _p)\n";
	}

	/** A Python string literal. */
	public static String quote(String s) {
		return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}

	/** The strings of a Python value (a str or a list of str). */
	public static List<String> strings(PyValue v) {
		List<String> out = new ArrayList<>();
		if (v instanceof PyValue.Str s) {
			out.addAll(List.of(s.values()));
		} else if (v instanceof PyValue.PyList l) {
			for (PyValue e : l.values()) {
				out.addAll(strings(e));
			}
		}
		return out;
	}
}
