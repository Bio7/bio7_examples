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
import org.eclipse.nebula.visualization.widgets.datadefinition.IManualValueChangeListener;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.KnobFigure;
import org.eclipse.nebula.visualization.widgets.figures.MeterFigure;
import org.eclipse.nebula.visualization.widgets.figures.ProgressBarFigure;
import org.eclipse.nebula.visualization.widgets.figures.ScaledSliderFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.nebula.visualization.widgets.figures.ThermometerFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.eco.bio7.collection.CustomView;

/**
 * An ecological monitoring station in a single Bio7 custom view, using a
 * {@link SashForm} instead of a plain grid so the user can resize the two
 * panels at runtime by dragging the divider.
 *
 * <pre>
 * +-------------------------------------------------------------+
 * |                 Ecological Monitoring Station                 |
 * +----------------+--------------------------------------------+
 * |  Soil Moisture |    Air Temperature   |  CO2 Concentration   |
 * |                |                      |                       |
 * | Vegetation     +----------------------+-----------------------+
 * | Cover          |   Stress Index       |   Soil Temperature    |
 * |                |   (drag me)          |                       |
 * | Water Table    |                      |                       |
 * | Level          |                      |                       |
 * +----------------+--------------------------------------------+
 * </pre>
 *
 * The left panel stacks 3 vertical habitat level indicators, the right panel
 * shows a 2 x 2 grid of round/linear environmental instruments. The knob on
 * the right simulates an overall environmental stress level and drives all
 * other instruments as long as the simulation is running - higher stress
 * dries out the soil, thins the vegetation cover, lowers the water table and
 * raises both air and soil temperature.
 */
public class ControlRoomExample extends com.eco.bio7.compile.Model {

	private static final int UPDATE_INTERVAL = 150;

	private Composite parent;

	private TankFigure soilMoisture;
	private ProgressBarFigure vegetationCover;
	private ScaledSliderFigure waterTableLevel;

	private GaugeFigure airTemperature;
	private MeterFigure co2Concentration;
	private KnobFigure stressIndex;
	private ThermometerFigure soilTemperature;

	private double t = 0;

	public void setup() {

		CustomView view = new CustomView();
		parent = view.getComposite("ControlRoomExample");
		parent.setLayout(new GridLayout(1, false));

		Label header = new Label(parent, SWT.CENTER);
		header.setText("Ecological Monitoring Station");
		header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createHabitatPanel(sash);
		createEnvironmentPanel(sash);

		/* Give the habitat panel roughly a third of the width. */
		sash.setWeights(new int[] { 35, 65 });

		/* The interactive knob drives all other instruments. */
		stressIndex.addManualValueChangeListener(new IManualValueChangeListener() {
			public void manualValueChanged(double newValue) {
				setAllValues(newValue);
			}
		});

		parent.layout();

		startLiveUpdate();
	}

	/** Left sash panel: 3 stacked vertical habitat level indicators. */
	private void createHabitatPanel(Composite sash) {

		Composite levels = new Composite(sash, SWT.TRANSPARENT | SWT.DOUBLE_BUFFERED);
		levels.setLayout(new GridLayout(1, true));

		soilMoisture = addWidget(levels, "Soil Moisture", new TankFigure());
		soilMoisture.setRange(0, 100);
		soilMoisture.setLoLevel(30);
		soilMoisture.setShowLo(true);
		soilMoisture.setFillColor(XYGraphMediaFactory.getInstance().getColor(139, 90, 43));

		vegetationCover = addWidget(levels, "Vegetation Cover", new ProgressBarFigure());
		vegetationCover.setRange(0, 100);
		vegetationCover.setFillColor(XYGraphMediaFactory.getInstance().getColor(0, 160, 60));

		waterTableLevel = addWidget(levels, "Water Table Level", new ScaledSliderFigure());
		waterTableLevel.setRange(0, 100);
		waterTableLevel.setFillColor(XYGraphMediaFactory.getInstance().getColor(0, 120, 255));
	}

	/** Right sash panel: 2 x 2 grid of round/linear environmental instruments. */
	private void createEnvironmentPanel(Composite sash) {

		Composite instruments = new Composite(sash, SWT.NONE);
		instruments.setLayout(new GridLayout(2, true));

		airTemperature = addWidget(instruments, "Air Temperature", new GaugeFigure());
		airTemperature.setUnit("°C");
		airTemperature.setRange(-10, 40);
		airTemperature.setHiLevel(25);
		airTemperature.setShowHi(true);
		airTemperature.setHiColor(ColorConstants.orange);
		airTemperature.setHihiLevel(35);
		airTemperature.setShowHihi(true);
		airTemperature.setHihiColor(ColorConstants.red);
		airTemperature.setNeedleColor(ColorConstants.red);

		co2Concentration = addWidget(instruments, "CO2 Concentration", new MeterFigure());
		co2Concentration.setRange(300, 1200);
		co2Concentration.setHiLevel(1000);
		co2Concentration.setShowHi(true);

		stressIndex = addWidget(instruments, "Stress Index (drag me)", new KnobFigure());
		stressIndex.setRange(0, 100);

		soilTemperature = addWidget(instruments, "Soil Temperature", new ThermometerFigure());
		soilTemperature.setRange(0, 40);
		soilTemperature.setFillColor(XYGraphMediaFactory.getInstance().getColor(204, 102, 0));
	}

	/**
	 * Creates a titled group with a canvas and places the figure inside it,
	 * filling the whole cell.
	 */
	private <T extends IFigure> T addWidget(Composite parent, String title, T figure) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		group.setLayout(new FillLayout());
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Canvas canvas = new Canvas(group, SWT.NONE);
		LightweightSystem lws = new LightweightSystem(canvas);
		lws.setContents(figure);

		return figure;
	}

	/** Derives every ecological reading from a single 0-100 stress level. */
	private void setAllValues(double stress) {
		soilMoisture.setValue(100 - stress * 0.6);
		vegetationCover.setValue(100 - stress * 0.8);
		waterTableLevel.setValue(100 - stress * 0.5);
		airTemperature.setValue(-10 + stress * 0.5);
		co2Concentration.setValue(300 + stress * 9);
		soilTemperature.setValue(stress * 0.4);
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
				t += 0.08;
				double stress = 50 + 45 * Math.sin(t);
				/* Drives the knob, which in turn notifies the listener below
				 * and propagates the value to all other instruments. */
				stressIndex.manualSetValue(stress);
				parent.getDisplay().timerExec(UPDATE_INTERVAL, this);
			}
		};

		parent.getDisplay().timerExec(UPDATE_INTERVAL, updater);
	}

}
