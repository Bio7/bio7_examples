package gui;

/********************************************************************************
 * Copyright (c) 2026 Marcel Austenfeld
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ********************************************************************************/

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.KnobFigure;
import org.eclipse.nebula.visualization.widgets.figures.MeterFigure;
import org.eclipse.nebula.visualization.widgets.figures.ProgressBarFigure;
import org.eclipse.nebula.visualization.widgets.figures.ScaledSliderFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.nebula.visualization.widgets.figures.ThermometerFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.collection.Work;

/**
 * A "stations" dashboard built from real Bio7 custom views arranged in the
 * built-in "Custom" perspective (com.eco.bio7.collection.CustomPerspective) -
 * unlike {@link GaugeDashboardExample} and {@link ControlRoomExample}, which
 * both arrange several widgets inside a single view.
 *
 * Each of the 4 view placeholders used ("left", "right", "subLeftTop",
 * "subLeftBottom") becomes one "station" of the vehicle: a titled Eclipse
 * view that itself hosts 2 related widgets side by side, so both the
 * perspective layout (which view goes where) and the layout inside each
 * view (a small 2 column grid of titled instruments) contribute to the
 * overall picture.
 *
 * <pre>
 * +------------------------+------------------------+
 * |        Engine          |         Cabin          |
 * |  RPM       Oil Temp    |  Cabin Temp  Fan Speed |
 * +------------------------+------------------------+
 * |      Fuel System       |       Drivetrain        |
 * |  Fuel     Efficiency   |  Throttle     Speed     |
 * +------------------------+------------------------+
 * </pre>
 */
public class StationsDashboardExample extends com.eco.bio7.compile.Model {

	private static final int UPDATE_INTERVAL = 150;

	private final List<Runnable> ticks = new ArrayList<Runnable>();

	private volatile boolean running = false;

	private double t = 0;

	public void setup() {

		/* Switch to the perspective which provides the view placeholders. */
		Work.openPerspective("com.eco.bio7.CustomPerspective");

		createEngineStation();
		createCabinStation();
		createFuelStation();
		createDrivetrainStation();

		running = true;
		scheduleTick();
	}

	/** Top left view: engine instruments. */
	private void createEngineStation() {

		Composite station = openStation("subLeftTop", "Engine");

		final GaugeFigure rpm = addWidget(station, "RPM", new GaugeFigure());
		rpm.setRange(0, 8000);
		rpm.setHiLevel(6000);
		rpm.setShowHi(true);
		rpm.setHiColor(ColorConstants.orange);
		rpm.setHihiLevel(7000);
		rpm.setShowHihi(true);
		rpm.setHihiColor(ColorConstants.red);
		ticks.add(() -> rpm.setValue(3500 + 2500 * Math.sin(t * 0.7)));

		final ThermometerFigure oilTemp = addWidget(station, "Oil Temp", new ThermometerFigure());
		oilTemp.setRange(0, 150);
		oilTemp.setHiLevel(120);
		oilTemp.setShowHi(true);
		oilTemp.setHiColor(ColorConstants.red);
		oilTemp.setFillColor(ColorConstants.orange);
		ticks.add(() -> oilTemp.setValue(90 + 20 * Math.sin(t * 0.3 + 1)));

		station.layout();
	}

	/** Top right view: cabin instruments. */
	private void createCabinStation() {

		Composite station = openStation("subLeftBottom", "Cabin");

		final MeterFigure cabinTemp = addWidget(station, "Cabin Temp", new MeterFigure());
		cabinTemp.setRange(-10, 40);
		ticks.add(() -> cabinTemp.setValue(20 + 8 * Math.sin(t * 0.25 + 2)));

		final KnobFigure fanSpeed = addWidget(station, "Fan Speed", new KnobFigure());
		fanSpeed.setRange(0, 100);
		ticks.add(() -> fanSpeed.manualSetValue(50 + 45 * Math.sin(t * 0.9 + 3)));

		station.layout();
	}

	/** Bottom left view: fuel system instruments. */
	private void createFuelStation() {

		Composite station = openStation("subRightTop", "Fuel System");

		final TankFigure fuel = addWidget(station, "Fuel Level", new TankFigure());
		fuel.setRange(0, 100);
		fuel.setLoLevel(20);
		fuel.setShowLo(true);
		fuel.setLoColor(ColorConstants.orange);
		fuel.setFillColor(ColorConstants.lightBlue);
		ticks.add(() -> fuel.setValue(50 - 20 * Math.sin(t * 0.05)));

		final ProgressBarFigure efficiency = addWidget(station, "Efficiency", new ProgressBarFigure());
		efficiency.setHorizontal(false);
		efficiency.setRange(0, 100);
		efficiency.setFillColor(ColorConstants.green);
		ticks.add(() -> efficiency.setValue(60 + 30 * Math.sin(t * 0.4 + 4)));

		station.layout();
	}

	/** Bottom right view: drivetrain instruments. */
	private void createDrivetrainStation() {

		Composite station = openStation("subRightBottom", "Drivetrain");

		final ScaledSliderFigure throttle = addWidget(station, "Throttle", new ScaledSliderFigure());
		throttle.setRange(0, 100);
		throttle.setFillColor(ColorConstants.yellow);
		ticks.add(() -> throttle.setValue(50 + 40 * Math.sin(t * 0.8 + 5)));

		final GaugeFigure speed = addWidget(station, "Speed", new GaugeFigure());
		speed.setUnit("km/h");
		speed.setRange(0, 220);
		speed.setHiLevel(160);
		speed.setShowHi(true);
		speed.setHiColor(ColorConstants.orange);
		ticks.add(() -> speed.setValue(110 + 90 * Math.sin(t * 0.6 + 6)));

		station.layout();
	}

	/**
	 * Creates (or reuses) a Bio7 custom view for the given secondary id - one of
	 * the placeholders defined in com.eco.bio7.collection.CustomPerspective - and
	 * gives it a 2 column grid layout ready to host 2 titled widgets.
	 */
	private Composite openStation(String secId, String title) {
		CustomView view = new CustomView(title);
		Composite parent = view.getComposite(secId);
		parent.setLayout(new GridLayout(2, true));
		return parent;
	}

	/**
	 * Creates a titled group with a canvas inside the given station and bridges
	 * it to Draw2D so the figure can be displayed.
	 */
	private <T extends IFigure> T addWidget(Composite station, String title, T figure) {

		Group group = new Group(station, SWT.NONE);
		group.setText(title);
		group.setLayout(new FillLayout());
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Canvas canvas = new Canvas(group, SWT.NONE);
		LightweightSystem lws = new LightweightSystem(canvas);
		lws.setContents(figure);

		return figure;
	}

	/** Runs all registered ticks and reschedules itself until {@link #close()}. */
	private void scheduleTick() {
		final Display display = CustomView.getDisplay();
		display.timerExec(UPDATE_INTERVAL, new Runnable() {
			public void run() {
				if (!running || display.isDisposed()) {
					return;
				}
				for (Runnable task : ticks) {
					task.run();
				}
				t += 0.15;
				display.timerExec(UPDATE_INTERVAL, this);
			}
		});
	}

	public void run() {
		/* Nothing to do - the animation runs via Display#timerExec (see setup). */
	}

	public void close() {
		running = false;
	}

}
