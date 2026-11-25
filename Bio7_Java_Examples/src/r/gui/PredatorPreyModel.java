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
 * A Java GUI for an R model: the Lotka-Volterra predator-prey model with a
 * carrying capacity of the prey, solved in R (Runge-Kutta 4, base R only). The
 * sliders set the parameters; the time series and the phase plane are drawn in
 * the R plot view, the minimum, maximum and the last values come back to the
 * GUI. With "Live" each change of a slider runs the model.
 *
 *   dN/dt = r N (1 - N / K) - a N P
 *   dP/dt = b a N P - m P
 *
 * The GUI is in PredatorPreyModelGui (WindowBuilder: Open With > WindowBuilder Editor).
 * Start R first (the R button of the toolbar).
 */
public class PredatorPreyModel extends com.eco.bio7.compile.Model {

	/** The model in R: the parameters in .p, the result in .result (min/max prey, min/max predators, the last values). */
	public static final String CODE = """
			.f <- function(s, p) c(p[1] * s[1] * (1 - s[1] / p[2]) - p[3] * s[1] * s[2],
			                       p[4] * p[3] * s[1] * s[2] - p[5] * s[2])
			.dt <- 0.05; .n <- round(.p[8] / .dt)
			.s <- matrix(NA, .n + 1, 2); .s[1, ] <- c(.p[6], .p[7])
			for (.i in 1:.n) {
			  .y <- .s[.i, ]
			  .k1 <- .f(.y, .p); .k2 <- .f(.y + .dt / 2 * .k1, .p)
			  .k3 <- .f(.y + .dt / 2 * .k2, .p); .k4 <- .f(.y + .dt * .k3, .p)
			  .s[.i + 1, ] <- pmax(0, .y + .dt / 6 * (.k1 + 2 * .k2 + 2 * .k3 + .k4))
			}
			.t <- seq(0, by = .dt, length.out = .n + 1)
			op <- par(mfrow = c(1, 2), mar = c(4.5, 4.5, 3, 1))
			matplot(.t, .s, type = "l", lty = 1, lwd = 2, col = c("#4C72B0", "#C44E52"),
			        xlab = "Time (years)", ylab = "Population", main = "Prey and predators")
			legend("topright", c("Prey", "Predators"), col = c("#4C72B0", "#C44E52"), lwd = 2, bty = "n")
			plot(.s[, 1], .s[, 2], type = "l", col = "#55A868", lwd = 2, xlab = "Prey", ylab = "Predators", main = "Phase plane")
			points(.s[1, 1], .s[1, 2], pch = 19); points(.s[.n + 1, 1], .s[.n + 1, 2], pch = 17, col = "#C44E52")
			# The equilibrium (if the predators can persist)
			.ne <- .p[5] / (.p[4] * .p[3])
			if (.ne < .p[2]) points(.ne, .p[1] / .p[3] * (1 - .ne / .p[2]), pch = 4, cex = 2, lwd = 2)
			par(op)
			.result <- c(range(.s[, 1]), range(.s[, 2]), .s[.n + 1, ])
			""";

	private PredatorPreyModelGui gui;
	private volatile boolean running;
	private volatile double[] pending;

	public PredatorPreyModel() {
		if (!RUtil.start()) {
			System.out.println("R could not be started (see the console)!");
			return;
		}
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Predator-Prey");
			parent.setLayout(new FillLayout());
			gui = new PredatorPreyModelGui(parent, PredatorPreyModel.this, SWT.NONE);
			parent.layout(true);
		});
	}

	/** p: r, K, a, b, m, N0, P0, years. A request during a run is kept (only the last one runs after it). */
	public void simulate(double... p) {
		if (running) {
			pending = p;
			return;
		}
		running = true;
		BackgroundJob.run("R model", () -> {
			RUtil.toR(".p", p);
			RUtil.exec(CODE);
			double[] r = RUtil.fromRDoubles(".result");
			String text = String.format(Locale.ROOT, "Prey: %.1f - %.1f, at the end %.1f%nPredators: %.1f - %.1f, at the end %.1f%s", r[0], r[1], r[4], r[2], r[3], r[5],
					r[5] < 0.01 ? "\nThe predators died out." : "");
			return text;
		}, status -> {
			running = false;
			if (!gui.isDisposed()) {
				gui.setResult(status);
			}
			double[] next = pending;
			pending = null;
			if (next != null) {
				simulate(next);
			}
		});
	}
}
