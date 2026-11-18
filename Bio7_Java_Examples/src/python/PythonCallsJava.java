package python;

import com.eco.bio7.pybridge.PythonCalls;
import com.eco.bio7.pybridge.PythonUtil;
import com.eco.bio7.pyffm.PyValue;

/*
 * Python calls Java: a Java function is registered for the embedded Python and
 * called in Python with bio7.call(name, *args, value=...). The arguments are
 * strings, the optional value any Python value (e.g., a numpy array). The
 * result is returned to Python.
 *
 * Here a growth model is simulated in Java and fitted in Python: Python calls
 * the Java simulation for each parameter value of the fit.
 * Needs numpy (scipy optional).
 */
public class PythonCallsJava {

	/* A logistic growth model (Java): the population sizes of the time steps */
	static double[] simulate(double r, double k, double n0, int steps) {
		double[] n = new double[steps];
		n[0] = n0;
		for (int t = 1; t < steps; t++) {
			n[t] = n[t - 1] + r * n[t - 1] * (1 - n[t - 1] / k);
		}
		return n;
	}

	public static void main(String[] args) throws Exception {

		/* bio7.call("simulate", r, k, n0, steps) in Python */
		PythonCalls.register("simulate", (String[] a, PyValue value) -> {
			double[] n = simulate(Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]), Integer.parseInt(a[3]));
			return PyValue.of(n);
		});

		/* bio7.call("java_sum", value=array): a value from Python to Java */
		PythonCalls.register("java_sum", (String[] a, PyValue value) -> {
			double s = 0;
			for (double d : ((PyValue.Array) value).asDoubles()) {
				s += d;
			}
			return PyValue.scalar(s);
		});

		/* Observations with noise (simulated with Java) to Python */
		double[] observed = simulate(0.3, 800, 10, 30);
		java.util.Random random = new java.util.Random(1);
		for (int i = 0; i < observed.length; i++) {
			observed[i] *= Math.exp(random.nextGaussian() * 0.05);
		}
		PythonUtil.toPython("observed", observed);

		/* Python fits the model of Java (the output in the console) */
		PythonUtil.run("""
				import numpy as np
				import bio7

				def model(r, k):
				    return bio7.call("simulate", r, k, 10, len(observed))

				def sse(p):
				    return float(np.sum((model(*p) - observed) ** 2))

				try:
				    from scipy.optimize import minimize
				    best = minimize(sse, x0=[0.1, 500], method="Nelder-Mead").x
				except ImportError:
				    grid = [(r, k) for r in np.linspace(0.05, 0.6, 56) for k in np.linspace(400, 1200, 81)]
				    best = min(grid, key=sse)
				r, k = best
				print(f"Fitted with the Java model: r = {r:.3f}, K = {k:.1f}  (simulated with r = 0.3, K = 800)")
				print("Sum computed in Java:", bio7.call("java_sum", value=np.arange(10.0)))

				import matplotlib.pyplot as plt
				plt.plot(observed, "o", label="observed")
				plt.plot(model(r, k), label="Java model (fitted)")
				plt.legend()
				plt.show()
				""");
	}
}
