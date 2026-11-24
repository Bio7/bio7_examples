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
 * examples (in the project Bio7_Python_Examples of the workspace) and the
 * conversion of Python values.
 */
public class PythonExamples {

	private static final java.util.Map<String, Path> CHOSEN = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * A folder of the Python examples, e.g. folder("OpenCV"): in the project
	 * Bio7_Python_Examples of the workspace, else selected once in a directory
	 * dialog of Bio7.
	 */
	public static Path folder(String name) {
		Path p = Path.of(FileRoot.getWorkspacePath(), "Bio7_Python_Examples", name);
		if (Files.isDirectory(p)) {
			return p;
		}
		Path chosen = CHOSEN.get(name);
		if (chosen == null) {
			String dir = Bio7Dialog.directory("Select the folder " + name + " of the Bio7 examples");
			if (dir == null) {
				throw new IllegalStateException("The folder " + name + " of the Python examples was not found (project Bio7_Python_Examples)");
			}
			chosen = Path.of(dir);
			CHOSEN.put(name, chosen);
		}
		return chosen;
	}

	/**
	 * Python code which makes the modules of a folder importable: the folder is
	 * the first of sys.path and the caches of the imports are cleared (the
	 * embedded Python keeps running, it may have looked into the folder before a
	 * module was copied into it).
	 */
	public static String addToPath(Path folder) {
		return "import sys, importlib\n_p = " + quote(folder.toString()) + "\nwhile _p in sys.path:\n    sys.path.remove(_p)\n"
				+ "sys.path.insert(0, _p)\nsys.path_importer_cache.pop(_p, None)\nimportlib.invalidate_caches()\n";
	}

	/**
	 * Python code which imports a module of a folder of the examples (as alias),
	 * a new import each time (changes of the module are used); a clear error if
	 * the module is not in the folder.
	 */
	public static String importModule(Path folder, String module, String alias) {
		return addToPath(folder) + "import os\nif not os.path.isfile(os.path.join(_p, " + quote(module + ".py") + ")):\n"
				+ "    raise FileNotFoundError(" + quote(module + ".py is not in the folder ") + " + _p)\n"
				+ "sys.modules.pop(" + quote(module) + ", None)\nimport " + module + " as " + alias + "\n";
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
