package gui.swt;

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

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.KnobFigure;
import org.eclipse.nebula.visualization.widgets.figures.MeterFigure;
import org.eclipse.nebula.visualization.widgets.figures.ProgressBarFigure;
import org.eclipse.nebula.visualization.widgets.figures.ScaledSliderFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.eco.bio7.collection.CustomView;

/**
 * A "hero" instrument-cluster dashboard in a single Bio7 custom view.
 *
 * Instead of a uniform grid of equally sized widgets, one primary gauge
 * ("Speed") is given twice the width and height of its neighbours by using
 * GridData column/row spans, so it visually dominates the layout the way a
 * car's central speedometer does. The fuel tank on the left also spans two
 * rows, and the throttle and engine load bars form full width strips across
 * the bottom.
 *
 * <pre>
 * +----------------------------------------------------------+
 * |                        Dashboard                          |
 * +--------+-----------------------------------+-------------+
 * |        |                                   | Temperature |
 * |  Fuel  |               Speed                +-------------+
 * |        |                                   |   Volume    |
 * +--------+-----------------------------------+-------------+
 * |                       Throttle                            |
 * +------------------------------------------------------------+
 * |                     Engine Load                             |
 * +------------------------------------------------------------+
 * </pre>
 */
public class GaugeDashboardExample extends com.eco.bio7.compile.Model {

	private static final int UPDATE_INTERVAL = 150;

	private Composite parent;

	private TankFigure fuel;
	private GaugeFigure speed;
	private MeterFigure temperature;
	private KnobFigure volume;
	private ScaledSliderFigure throttle;
	private ProgressBarFigure engineLoad;

	private double t = 0;

	public void setup() {

		CustomView view = new CustomView();
		parent = view.getComposite("GaugeDashboardExample");

		GridLayout layout = new GridLayout(4, false);
		layout.horizontalSpacing = 8;
		layout.verticalSpacing = 8;
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		parent.setLayout(layout);

		Label header = new Label(parent, SWT.CENTER);
		header.setText("Vehicle Dashboard");
		GridData headerData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		headerData.horizontalSpan = 4;
		header.setLayoutData(headerData);

		/* Left column: a tall fuel tank spanning both instrument rows. */
		fuel = addWidget("Fuel Level", new TankFigure(), 1, 2);
		fuel.setRange(0, 100);
		fuel.setFillColor(ColorConstants.lightBlue);
		fuel.setLoLevel(20);
		fuel.setShowLo(true);
		fuel.setLoColor(ColorConstants.orange);

		/* Center: the hero gauge, twice as wide and twice as tall. */
		speed = addWidget("Speed", new GaugeFigure(), 2, 2);
		speed.setUnit("km/h");
		speed.setRange(0, 220);
		speed.setHiLevel(160);
		speed.setShowHi(true);
		speed.setHiColor(ColorConstants.orange);
		speed.setHihiLevel(200);
		speed.setShowHihi(true);
		speed.setHihiColor(ColorConstants.red);
		speed.setEffect3D(true);

		/* Right column, top: temperature. */
		temperature = addWidget("Temperature", new MeterFigure(), 1, 1);
		temperature.setRange(-20, 50);
		temperature.setHiLevel(35);
		temperature.setShowHi(true);
		temperature.setHiColor(ColorConstants.orange);

		/* Right column, bottom: volume (falls into the remaining free cell). */
		volume = addWidget("Volume", new KnobFigure(), 1, 1);
		volume.setRange(0, 100);

		/* Full width strip: throttle. */
		throttle = addWidget("Throttle", new ScaledSliderFigure(), 4, 1);
		throttle.setHorizontal(true);
		throttle.setRange(0, 100);
		throttle.setFillColor(ColorConstants.yellow);

		/* Full width strip: engine load. */
		engineLoad = addWidget("Engine Load", new ProgressBarFigure(), 4, 1);
		engineLoad.setHorizontal(true);
		engineLoad.setRange(0, 100);
		engineLoad.setFillColor(ColorConstants.green);

		/* Here we have to call the layout method! */
		parent.layout();

		startLiveUpdate();
	}

	/**
	 * Creates a titled group of the given grid span, a Canvas inside it and
	 * bridges the Canvas to Draw2D so the figure can be displayed.
	 */
	private <T extends IFigure> T addWidget(String title, T figure, int horizontalSpan, int verticalSpan) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		group.setLayout(new FillLayout());

		/* Every control needs its own GridData instance! */
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = horizontalSpan;
		gd.verticalSpan = verticalSpan;
		group.setLayoutData(gd);

		Canvas canvas = new Canvas(group, SWT.NONE);
		LightweightSystem lws = new LightweightSystem(canvas);
		lws.setContents(figure);

		return figure;
	}

	/**
	 * Schedules a repeating update in the UI thread. The loop stops automatically
	 * as soon as the custom view (and thus the parent composite) has been
	 * disposed.
	 */
	private void startLiveUpdate() {

		final Runnable updater = new Runnable() {
			public void run() {
				if (parent == null || parent.isDisposed()) {
					return;
				}
				updateValues();
				t += 0.1;
				parent.getDisplay().timerExec(UPDATE_INTERVAL, this);
			}
		};

		parent.getDisplay().timerExec(UPDATE_INTERVAL, updater);
	}

	/** Simulated driving data - replace this with your own data source. */
	private void updateValues() {

		speed.setValue(110 + 90 * Math.sin(t * 0.6));
		temperature.setValue(15 + 20 * Math.sin(t * 0.35 + 1));
		volume.manualSetValue(50 + 45 * Math.sin(t * 0.9 + 2));
		fuel.setValue(50 - 20 * Math.sin(t * 0.05));
		throttle.setValue(50 + 40 * Math.sin(t * 0.8 + 3));
		engineLoad.setValue(50 + 45 * Math.sin(t * 0.7 + 4));
	}

}
