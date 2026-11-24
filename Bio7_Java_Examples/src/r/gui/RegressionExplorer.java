package r.gui;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.rbridge.RUtil;

import gui.swt.BackgroundJob;

/*
 * A Java GUI for R: a data set of R (cars, trees, mtcars, iris, airquality,
 * faithful), two numeric variables and a model (linear, polynomial,
 * logarithmic, loess). R fits the model, draws the data with the fit and the
 * confidence band and the residuals in the R plot view, the summary of the
 * model is shown in the GUI.
 *
 * The GUI is in RegressionExplorerGui (WindowBuilder: Open With > WindowBuilder Editor).
 * Start R first (the R button of the toolbar).
 */
public class RegressionExplorer extends com.eco.bio7.compile.Model {

	private RegressionExplorerGui gui;

	public RegressionExplorer() {
		if (!RUtil.isAlive()) {
			System.out.println("The embedded R is not running: please start R first!");
			return;
		}
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Regression");
			parent.setLayout(new FillLayout());
			gui = new RegressionExplorerGui(parent, RegressionExplorer.this, SWT.NONE);
			parent.layout(true);
			dataSet("cars");
		});
	}

	/** The numeric variables of a data set (for the combos of x and y). */
	public void dataSet(String name) {
		BackgroundJob.run("R data set", () -> {
			String[] vars = RUtil.fromRStrings("names(Filter(is.numeric, " + name + "))");
			Display.getDefault().asyncExec(() -> gui.setVariables(vars));
			return name + ": " + RUtil.fromRDouble("nrow(" + name + ")") + " rows, " + vars.length + " numeric variables";
		}, this::done);
	}

	/** model: 0 linear, 1 polynomial (degree), 2 logarithmic (y ~ log(x)), 3 loess (span) */
	public void fit(String data, String x, String y, int model, int degree, double span) {
		if (x.isEmpty() || y.isEmpty()) {
			return;
		}
		gui.setBusy(true);
		BackgroundJob.run("R regression", () -> {
			RUtil.exec(code(data, x, y, model, degree, span));
			String summary = RUtil.print("summary(.fit)");
			String extra = model == 3 ? "" : RUtil.fromRString("sprintf('AIC %.2f   R-squared %.3f', AIC(.fit), summary(.fit)$r.squared)") + "\n\n";
			Display.getDefault().asyncExec(() -> gui.setSummary(extra + summary));
			return "Fitted: " + RUtil.fromRString("paste(deparse(formula(.fit)), collapse = '')") + (model == 3 ? " (loess)" : "");
		}, this::done);
	}

	/** The R code: the model fitted (.fit) and the plots. */
	public static String code(String data, String x, String y, int model, int degree, double span) {
		String formula = switch (model) {
		case 1 -> String.format("%s ~ poly(%s, %d, raw = TRUE)", y, x, degree);
		case 2 -> String.format("%s ~ log(%s)", y, x);
		default -> y + " ~ " + x;
		};
		String fit = model == 3 ? String.format(Locale.ROOT, "loess(%s ~ %s, data = .d, span = %.2f)", y, x, span) : "lm(" + formula + ", data = .d)";
		return String.format(Locale.ROOT, """
				.d <- na.omit(%1$s[, c("%2$s", "%3$s")])
				.d <- .d[if (%4$d == 2) .d$%2$s > 0 else TRUE, ]
				.fit <- %5$s
				.grid <- data.frame(%2$s = seq(min(.d$%2$s), max(.d$%2$s), length.out = 200))
				.pred <- predict(.fit, .grid, se = TRUE)
				.se <- if (is.list(.pred)) .pred$se.fit else NA
				.fitted <- if (is.list(.pred)) .pred$fit else .pred
				op <- par(mfrow = c(1, 2), mar = c(4.5, 4.5, 3, 1))
				plot(.d$%2$s, .d$%3$s, pch = 19, col = adjustcolor("#4C72B0", 0.6), xlab = "%2$s", ylab = "%3$s",
				     main = paste("%1$s:", deparse(formula(.fit))))
				if (!all(is.na(.se))) polygon(c(.grid$%2$s, rev(.grid$%2$s)), c(.fitted + 1.96 * .se, rev(.fitted - 1.96 * .se)),
				     col = adjustcolor("#C44E52", 0.2), border = NA)
				lines(.grid$%2$s, .fitted, col = "#C44E52", lwd = 2)
				plot(fitted(.fit), residuals(.fit), pch = 19, col = adjustcolor("#55A868", 0.6), xlab = "Fitted", ylab = "Residuals", main = "Residuals")
				abline(h = 0, lty = 2)
				par(op)
				""", data, x, y, model, fit);
	}

	private void done(String status) {
		if (!gui.isDisposed()) {
			gui.setStatus(status);
			gui.setBusy(false);
		}
	}
}
