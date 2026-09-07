package gui;

import java.util.Random;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.eco.bio7.collection.CustomView;

/**
 * Six live updated gauges in a Bio7 custom view (3 x 2 grid).
 *
 * Each gauge sits on its own Canvas which is bridged to Draw2D with a
 * LightweightSystem. The values are driven by a simple random walk which is
 * scheduled with Display#timerExec, so the update runs in the SWT UI thread.
 */
public class MultipleGaugesExample extends com.eco.bio7.compile.Model {

	/** Number of gauges. */
	private static final int COUNT = 6;

	/** Update interval of the simulated values in milliseconds. */
	private static final int UPDATE_INTERVAL = 500;

	private final String[] titles = { "Temperature (\u00B0C)", "Humidity (%)", "Pressure (hPa)", "CO2 (ppm)",
			"Light (klx)", "Wind (m/s)" };

	private final double[] minima = { -20, 0, 950, 300, 0, 0 };
	private final double[] maxima = { 60, 100, 1050, 2000, 120, 40 };

	private final GaugeFigure[] gauges = new GaugeFigure[COUNT];
	private final double[] values = new double[COUNT];

	private final Random random = new Random();
	private Composite parent;

	public void setup() {

		CustomView view = new CustomView();
		parent = view.getComposite("MultipleGaugesExample");

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		parent.setLayout(layout);

		for (int i = 0; i < COUNT; i++) {

			/* A group gives us a titled frame around each gauge. */
			Group group = new Group(parent, SWT.NONE);
			group.setText(titles[i]);
			group.setLayout(new FillLayout());

			/*
			 * Important: every control needs its OWN GridData instance - a shared instance
			 * is only honoured for the control it was set on last.
			 */
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			/* Create the canvas and the SWT <-> Draw2D bridge. */
			Canvas gaugeCanvas = new Canvas(group, SWT.TRANSPARENT | SWT.DOUBLE_BUFFERED);
			LightweightSystem lws = new LightweightSystem(gaugeCanvas);

			GaugeFigure gauge = new GaugeFigure();
			gauge.setBackgroundColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
			gauge.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
			gauge.setNeedleColor(XYGraphMediaFactory.getInstance().getColor(255, 0, 0));

			double min = minima[i];
			double max = maxima[i];
			double span = max - min;

			gauge.setRange(min, max);
			gauge.setLoloLevel(min + 0.10 * span);
			gauge.setLoLevel(min + 0.20 * span);
			gauge.setHiLevel(min + 0.80 * span);
			gauge.setHihiLevel(min + 0.90 * span);
			gauge.setShowMarkers(true);
			gauge.setEffect3D(true);

			values[i] = min + 0.5 * span;
			gauge.setValue(values[i]);

			lws.setContents(gauge);
			gauges[i] = gauge;
		}

		/* Here we have to call the layout method! */
		parent.layout();

		startLiveUpdate();
	}

	/**
	 * Schedules a repeating update in the UI thread. The loop stops automatically
	 * as soon as the custom view (and thus the parent composite) has been disposed.
	 */
	private void startLiveUpdate() {

		final Runnable updater = new Runnable() {
			public void run() {
				if (parent == null || parent.isDisposed()) {
					return;
				}
				updateValues();
				parent.getDisplay().timerExec(UPDATE_INTERVAL, this);
			}
		};

		parent.getDisplay().timerExec(UPDATE_INTERVAL, updater);
	}

	/** Random walk - replace this with your own data source. */
	private void updateValues() {

		for (int i = 0; i < COUNT; i++) {

			double span = maxima[i] - minima[i];
			values[i] += (random.nextDouble() - 0.5) * span * 0.08;

			if (values[i] < minima[i]) {
				values[i] = minima[i];
			}
			if (values[i] > maxima[i]) {
				values[i] = maxima[i];
			}

			gauges[i].setValue(values[i]);
		}
	}
}