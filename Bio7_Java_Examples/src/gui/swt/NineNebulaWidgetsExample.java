package gui.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.nebula.visualization.widgets.datadefinition.IManualValueChangeListener;
import org.eclipse.nebula.visualization.widgets.figures.AbstractMarkedWidgetFigure;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.KnobFigure;
import org.eclipse.nebula.visualization.widgets.figures.MeterFigure;
import org.eclipse.nebula.visualization.widgets.figures.ProgressBarFigure;
import org.eclipse.nebula.visualization.widgets.figures.ScaledSliderFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.nebula.visualization.widgets.figures.ThermometerFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;

import com.eco.bio7.collection.CustomView;

/**
 * Nine Nebula visualization widgets in a Bio7 custom view (3 x 3), each one
 * centered inside its cell.
 *
 * Centering is done in Draw2D: instead of handing the widget figure directly to
 * the LightweightSystem (which would stretch it over the whole canvas), the
 * widget is placed into a container figure with an XYLayout. A resize listener
 * then computes the largest rectangle of the desired aspect ratio that fits
 * into the canvas and centers it. This keeps the round widgets circular.
 */
public class NineNebulaWidgetsExample extends com.eco.bio7.compile.Model {

	private static final double MIN = 0;
	private static final double MAX = 100;
	private static final int UPDATE_INTERVAL = 200;

	/** Aspect ratio (width / height) of the different widget shapes. */
	private static final double ROUND = 1.0;
	private static final double VERTICAL = 0.65;
	private static final double HORIZONTAL = 2.5;

	private final List<AbstractMarkedWidgetFigure> widgets = new ArrayList<AbstractMarkedWidgetFigure>();

	private Composite parent;
	private Button liveButton;
	private boolean live = true;
	private double phase = 0;

	public void setup() {

		CustomView view = new CustomView();
		parent = view.getComposite("NineNebulaWidgetsExample");

		parent.setLayout(new GridLayout(3, true));

		createHeader();

		/* --- 1. Gauge (dark theme) ------------------------------------ */
		GaugeFigure gauge = new GaugeFigure();
		gauge.setBackgroundColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
		gauge.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		gauge.setNeedleColor(XYGraphMediaFactory.getInstance().getColor(255, 0, 0));
		gauge.setEffect3D(true);
		register("Gauge (dark)", gauge, ROUND);

		/* --- 2. Gauge (flat, light) ----------------------------------- */
		GaugeFigure flatGauge = new GaugeFigure();
		flatGauge.setNeedleColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 200));
		flatGauge.setEffect3D(false);
		register("Gauge (flat)", flatGauge, ROUND);

		/* --- 3. Meter ------------------------------------------------- */
		MeterFigure meter = new MeterFigure();
		meter.setNeedleColor(XYGraphMediaFactory.getInstance().getColor(255, 0, 0));
		register("Meter", meter, ROUND);

		/* --- 4. Knob (interactive) ------------------------------------ */
		final KnobFigure knob = new KnobFigure();
		register("Knob (drag me)", knob, ROUND);

		/* --- 5. Thermometer ------------------------------------------- */
		ThermometerFigure thermo = new ThermometerFigure();
		thermo.setFillColor(XYGraphMediaFactory.getInstance().getColor(255, 60, 60));
		register("Thermometer", thermo, VERTICAL);

		/* --- 6. Tank -------------------------------------------------- */
		TankFigure tank = new TankFigure();
		tank.setFillColor(XYGraphMediaFactory.getInstance().getColor(0, 120, 255));
		tank.setEffect3D(true);
		register("Tank", tank, VERTICAL);

		/* --- 7. Progress bar (horizontal) ----------------------------- */
		ProgressBarFigure horizontalBar = new ProgressBarFigure();
		horizontalBar.setHorizontal(true);
		horizontalBar.setFillColor(XYGraphMediaFactory.getInstance().getColor(0, 180, 0));
		register("Progress bar (horizontal)", horizontalBar, HORIZONTAL);

		/* --- 8. Progress bar (vertical) ------------------------------- */
		ProgressBarFigure verticalBar = new ProgressBarFigure();
		verticalBar.setHorizontal(false);
		verticalBar.setFillColor(XYGraphMediaFactory.getInstance().getColor(180, 0, 180));
		register("Progress bar (vertical)", verticalBar, VERTICAL);

		/* --- 9. Scaled slider (interactive) --------------------------- */
		final ScaledSliderFigure slider = new ScaledSliderFigure();
		slider.setHorizontal(false);
		slider.setFillColor(XYGraphMediaFactory.getInstance().getColor(255, 160, 0));
		register("Scaled slider (drag me)", slider, VERTICAL);

		/* The interactive widgets drive all the others. */
		IManualValueChangeListener manualListener = new IManualValueChangeListener() {
			public void manualValueChanged(double newValue) {
				live = false;
				liveButton.setSelection(false);
				setAllValues(newValue);
			}
		};
		knob.addManualValueChangeListener(manualListener);
		slider.addManualValueChangeListener(manualListener);

		/* Here we have to call the layout method! */
		parent.layout();

		startLiveUpdate();
	}

	/** A centered check box spanning all three columns. */
	private void createHeader() {

		liveButton = new Button(parent, SWT.CHECK);
		liveButton.setText("Live simulation (switch off to control the widgets with the knob or the slider)");
		liveButton.setSelection(true);

		GridData headerData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		headerData.horizontalSpan = 3;
		liveButton.setLayoutData(headerData);

		liveButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				live = liveButton.getSelection();
			}
		});
	}

	/**
	 * Creates a titled group with a canvas and places the widget figure
	 * centered on it.
	 *
	 * @param aspectRatio width / height of the widget shape
	 */
	private void register(String title, final AbstractMarkedWidgetFigure figure, final double aspectRatio) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		group.setLayout(new FillLayout());
		/* Every control needs its own GridData instance! */
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Canvas canvas = new Canvas(group, SWT.TRANSPARENT | SWT.DOUBLE_BUFFERED);

		figure.setRange(MIN, MAX);
		figure.setValue(MIN + 0.5 * (MAX - MIN));

		/* Optional decoration - remove this call and the method below if the
		 * marker API is missing in your Nebula build. */
		configureMarkers(figure);

		/* The container centers the widget instead of stretching it. */
		final Figure container = new Figure();
		container.setLayoutManager(new XYLayout());
		container.add(figure, new Rectangle(0, 0, 1, 1));

		/* The bridge between SWT and Draw2D. */
		LightweightSystem lws = new LightweightSystem(canvas);
		lws.setContents(container);

		canvas.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				centerFigure(canvas, container, figure, aspectRatio);
			}
		});
		centerFigure(canvas, container, figure, aspectRatio);

		widgets.add(figure);
	}

	/**
	 * Computes the largest rectangle with the given aspect ratio that fits into
	 * the canvas and places the figure centered into it.
	 */
	private void centerFigure(Canvas canvas, Figure container, AbstractMarkedWidgetFigure figure, double aspectRatio) {

		Point size = canvas.getSize();
		if (size.x <= 0 || size.y <= 0) {
			return;
		}

		int width = size.x;
		int height = (int) Math.round(width / aspectRatio);

		if (height > size.y) {
			height = size.y;
			width = (int) Math.round(height * aspectRatio);
		}

		int x = (size.x - width) / 2;
		int y = (size.y - height) / 2;

		container.setConstraint(figure, new Rectangle(x, y, width, height));
		container.revalidate();
	}

	/** Alarm levels and markers - declared in AbstractMarkedWidgetFigure. */
	private void configureMarkers(AbstractMarkedWidgetFigure figure) {

		double span = MAX - MIN;

		figure.setLoloLevel(MIN + 0.10 * span);
		figure.setLoLevel(MIN + 0.25 * span);
		figure.setHiLevel(MIN + 0.75 * span);
		figure.setHihiLevel(MIN + 0.90 * span);
		figure.setShowMarkers(true);
	}

	private void setAllValues(double value) {
		for (int i = 0; i < widgets.size(); i++) {
			widgets.get(i).setValue(value);
		}
	}

	/**
	 * Schedules a repeating update in the UI thread. The loop stops as soon as
	 * the custom view has been disposed.
	 */
	private void startLiveUpdate() {

		final Runnable updater = new Runnable() {
			public void run() {
				if (parent == null || parent.isDisposed()) {
					return;
				}
				if (live) {
					phase += 0.08;
					double value = MIN + (MAX - MIN) * (0.5 + 0.5 * Math.sin(phase));
					setAllValues(value);
				}
				parent.getDisplay().timerExec(UPDATE_INTERVAL, this);
			}
		};

		parent.getDisplay().timerExec(UPDATE_INTERVAL, updater);
	}
}