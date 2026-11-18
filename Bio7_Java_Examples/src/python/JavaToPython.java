package python;

import java.util.LinkedHashMap;
import java.util.Map;

import com.eco.bio7.pybridge.PythonUtil;
import com.eco.bio7.pyffm.PyValue;

/*
 * Transfers data from Java to the embedded Python of Bio7 and the results back
 * to Java (numbers, matrices, strings, dicts and pandas DataFrames).
 *
 * The values are created in the workspace of Python (__main__): they are
 * available in the Python console, the Python-Shell view and Python scripts.
 * Python is started if necessary (the Python of the preferences).
 * Needs numpy (and pandas for the DataFrame).
 */
public class JavaToPython {

	public static void main(String[] args) throws Exception {

		/* ---------- Java -> Python ---------- */

		/* Numbers: a numpy array (float64) */
		double[] temperature = { 12.1, 14.3, 13.8, 17.2, 19.5, 21.0, 18.4 };
		PythonUtil.toPython("temperature", temperature);

		/* Integers: a numpy array (int32) */
		PythonUtil.toPython("counts", 4, 8, 15, 16, 23, 42);

		/* One string: a str, several strings: a list */
		PythonUtil.toPython("site", "Lake Constance");
		PythonUtil.toPython("species", "Daphnia", "Bosmina", "Cyclops");

		/* A matrix (rows): a 2-d numpy array */
		double[][] matrix = { { 1, 2, 3 }, { 4, 5, 6 } };
		PythonUtil.toPython("m", matrix);

		/* A dict (PyValue.scalar: a number, PyValue.of: a numpy array) */
		Map<String, PyValue> settings = new LinkedHashMap<>();
		settings.put("depth", PyValue.scalar(2.5));
		settings.put("method", PyValue.of("net"));
		PythonUtil.toPython("settings", new PyValue.PyDict(settings));

		/* A pandas DataFrame (columns of the same length) */
		Map<String, PyValue> columns = new LinkedHashMap<>();
		columns.put("day", PyValue.of(1, 2, 3, 4, 5, 6, 7));
		columns.put("temperature", PyValue.of(temperature));
		columns.put("sunny", PyValue.of(false, true, true, false, true, true, false));
		PythonUtil.toPython("weather", new PyValue.Frame(columns, null));

		/* ---------- Computed in Python ---------- */

		PythonUtil.exec("""
				import numpy as np
				stats = {"mean": float(temperature.mean()), "sd": float(temperature.std(ddof=1))}
				m2 = m @ m.T                       # matrix product (2 x 2)
				label = f"{site}: {len(species)} species, depth {settings['depth']} m"
				warm = weather[weather.temperature > 15]
				""");

		/* ---------- Python -> Java ---------- */

		/* An expression: numbers as double[] */
		double[] scaled = PythonUtil.fromPythonDoubles("(temperature - temperature.mean()) / temperature.std()");
		System.out.println("Scaled temperature: " + java.util.Arrays.toString(scaled));

		/* A scalar */
		double sum = ((PyValue.Array) PythonUtil.fromPython("counts.sum()")).asDouble();
		System.out.println("Sum of the counts: " + sum);

		/* A matrix */
		double[][] product = ((PyValue.Array) PythonUtil.fromPython("m2")).asDoubleMatrix();
		System.out.println("m @ m.T: " + java.util.Arrays.deepToString(product));

		/* A string */
		String label = ((PyValue.Str) PythonUtil.fromPython("label")).asString();
		System.out.println(label);

		/* A dict */
		PyValue.PyDict stats = (PyValue.PyDict) PythonUtil.fromPython("stats");
		System.out.println("Mean: " + ((PyValue.Array) stats.get("mean")).asDouble() + ", sd: " + ((PyValue.Array) stats.get("sd")).asDouble());

		/* A DataFrame: the columns */
		PyValue.Frame warm = (PyValue.Frame) PythonUtil.fromPython("warm");
		double[] days = ((PyValue.Array) warm.column("day")).asDoubles();
		System.out.println("Warm days: " + java.util.Arrays.toString(days));

		/* Any value as text (the repr of Python) */
		System.out.println(((PyValue.Str) PythonUtil.fromPython("repr(weather.describe().round(2))")).asString());
	}
}
