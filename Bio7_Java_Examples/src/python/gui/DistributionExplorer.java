package python.gui;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.pybridge.PythonUtil;

import gui.BackgroundJob;
import python.PythonExamples;

/*
 * A Java GUI for Python: random samples of a distribution (numpy) as
 * histogram with the density and a QQ plot (matplotlib) in the Python Plot
 * view. The sliders change the parameters, with "Live" each change redraws.
 * The summary statistics come back from Python into the GUI.
 *
 * The GUI is in DistributionExplorerGui (WindowBuilder: Open With > WindowBuilder Editor).
 * Requires: numpy, matplotlib (Python Packages: the bundle "Scientific").
 */
public class DistributionExplorer extends com.eco.bio7.compile.Model {

	private DistributionExplorerGui gui;
	private volatile boolean running;
	private volatile String pending;

	public DistributionExplorer() {
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Distributions");
			parent.setLayout(new FillLayout());
			gui = new DistributionExplorerGui(parent, DistributionExplorer.this, SWT.NONE);
			parent.layout(true);
		});
	}

	/**
	 * Draws the distribution: name (normal, lognormal, gamma, exponential,
	 * poisson), the sample size, two parameters and the number of bins. A request
	 * during a drawing is kept (only the last one is drawn after it).
	 */
	public void plot(String distribution, int n, double p1, double p2, int bins) {
		String code = code(distribution, n, p1, p2, bins);
		if (running) {
			pending = code;
			return;
		}
		draw(code);
	}

	/** The Python code of a plot. */
	public static String code(String distribution, int n, double p1, double p2, int bins) {
		return String.format(Locale.ROOT, """
				import numpy as np
				import matplotlib.pyplot as plt
				_rng = np.random.default_rng(1)
				_d, _n, _p1, _p2, _bins = %s, %d, %f, %f, %d
				_x = {"normal": lambda: _rng.normal(_p1, _p2, _n),
				      "lognormal": lambda: _rng.lognormal(np.log(max(_p1, 1e-6)), _p2, _n),
				      "gamma": lambda: _rng.gamma(_p1, _p2, _n),
				      "exponential": lambda: _rng.exponential(_p1, _n),
				      "poisson": lambda: _rng.poisson(_p1, _n).astype(float)}[_d]()
				plt.close("all")
				_fig, (_a1, _a2) = plt.subplots(1, 2, figsize=(10, 4), constrained_layout=True)
				_a1.hist(_x, bins=_bins, density=True, color="#4C72B0", alpha=0.75, edgecolor="white")
				_a1.axvline(_x.mean(), color="#C44E52", lw=2, label="mean %%.2f" %% _x.mean())
				_a1.axvline(np.median(_x), color="#55A868", lw=2, ls="--", label="median %%.2f" %% np.median(_x))
				_a1.set(title="%%s (n = %%d)" %% (_d, _n), xlabel="x", ylabel="density")
				_a1.legend(frameon=False)
				# The QQ plot against the normal distribution (numpy only)
				_q = np.sort((_x - _x.mean()) / _x.std())
				from statistics import NormalDist
				_theory = np.array([NormalDist().inv_cdf(p) for p in (np.arange(1, _n + 1) - 0.5) / _n])
				_a2.scatter(_theory, _q, s=6, color="#4C72B0", alpha=0.6)
				_a2.plot([-4, 4], [-4, 4], color="gray", lw=1)
				_a2.set(title="QQ plot (standardized)", xlabel="normal quantiles", ylabel="sample quantiles", xlim=(-4, 4), ylim=(-4, 6))
				plt.show()
				_skew = float(((_x - _x.mean()) ** 3).mean() / _x.std() ** 3)
				_summary = "mean %%.3f, sd %%.3f, median %%.3f, min %%.3f, max %%.3f, skewness %%.2f" %% (
				    _x.mean(), _x.std(ddof=1), np.median(_x), _x.min(), _x.max(), _skew)
				""", "'" + distribution + "'", n, p1, p2, bins);
	}

	private void draw(String code) {
		running = true;
		BackgroundJob.run("Distribution", () -> {
			/* exec: the code runs completely before the summary is read (plt.show(): the plot view) */
			PythonUtil.exec(code);
			return String.join("", PythonExamples.strings(PythonUtil.fromPython("_summary")));
		}, status -> {
			running = false;
			if (!gui.isDisposed()) {
				gui.setSummary(status);
			}
			String next = pending;
			pending = null;
			if (next != null) {
				draw(next);
			}
		});
	}
}
