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
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.collection.Work;

/**
 * Bio7 API example: opens the built-in "Custom" perspective
 * (com.eco.bio7.collection.CustomPerspective) and creates 8 custom views
 * (com.eco.bio7.collection.CustomView) - one for each of the 8 view
 * placeholders defined in that perspective - each hosting an animated Nebula
 * Visualization widget (org.eclipse.nebula.visualization.widgets).
 *
 * See also "Gauges_4_Views" for a smaller variant using only 4 of the 8 view
 * placeholders.
 */
public class Gauges_8_Views extends com.eco.bio7.compile.Model {

	private static final int UPDATE_INTERVAL = 150;

	private final List<Runnable> ticks = new ArrayList<Runnable>();

	private volatile boolean running = false;

	private double t = 0;

	public void setup() {

		/* Switch to the perspective which provides the 8 view placeholders. */
		Work.openPerspective("com.eco.bio7.CustomPerspective");

		final GaugeFigure speed = show("left", "Speed", new GaugeFigure());
		speed.setUnit("km/h");
		speed.setRange(0, 220);
		speed.setHiLevel(160);
		speed.setShowHi(true);
		speed.setHiColor(ColorConstants.orange);
		speed.setHihiLevel(200);
		speed.setShowHihi(true);
		speed.setHihiColor(ColorConstants.red);
		ticks.add(() -> speed.setValue(110 + 90 * Math.sin(t * 0.6)));

		final MeterFigure temperature = show("right", "Temperature", new MeterFigure());
		temperature.setRange(-20, 50);
		temperature.setHiLevel(35);
		temperature.setShowHi(true);
		temperature.setHiColor(ColorConstants.orange);
		ticks.add(() -> temperature.setValue(15 + 20 * Math.sin(t * 0.35 + 1)));

		final KnobFigure volume = show("subLeftTop", "Volume", new KnobFigure());
		volume.setRange(0, 100);
		ticks.add(() -> volume.manualSetValue(50 + 45 * Math.sin(t * 0.9 + 2)));

		final ThermometerFigure coreTemp = show("subLeftBottom", "Core Temperature", new ThermometerFigure());
		coreTemp.setRange(0, 120);
		coreTemp.setHiLevel(90);
		coreTemp.setShowHi(true);
		coreTemp.setHiColor(ColorConstants.red);
		coreTemp.setFillColor(ColorConstants.red);
		ticks.add(() -> coreTemp.setValue(60 + 30 * Math.sin(t * 0.5 + 3)));

		final TankFigure fuel = show("subRightTop", "Fuel Level", new TankFigure());
		fuel.setRange(0, 100);
		fuel.setLoLevel(20);
		fuel.setShowLo(true);
		fuel.setLoColor(ColorConstants.orange);
		fuel.setFillColor(ColorConstants.lightBlue);
		ticks.add(() -> fuel.setValue(50 + 45 * Math.sin(t * 0.2 + 4)));

		final ProgressBarFigure progress = show("subRightBottom", "Task Progress", new ProgressBarFigure());
		progress.setRange(0, 100);
		progress.setFillColor(ColorConstants.green);
		ticks.add(() -> progress.setValue((t * 6) % 100));

		final ScaledSliderFigure throttle = show("subRightBottomLeft", "Throttle", new ScaledSliderFigure());
		throttle.setRange(0, 100);
		throttle.setFillColor(ColorConstants.yellow);
		ticks.add(() -> throttle.setValue(50 + 40 * Math.sin(t * 0.8 + 5)));

		final GaugeFigure pressure = show("subRightBottomRight", "Pressure", new GaugeFigure());
		pressure.setUnit("bar");
		pressure.setRange(0, 10);
		pressure.setHiLevel(7);
		pressure.setShowHi(true);
		pressure.setHiColor(ColorConstants.orange);
		pressure.setHihiLevel(9);
		pressure.setShowHihi(true);
		pressure.setHihiColor(ColorConstants.red);
		pressure.setNeedleColor(ColorConstants.blue);
		ticks.add(() -> pressure.setValue(5 + 4 * Math.sin(t * 0.7 + 6)));

		/* Advance the shared clock last so every widget uses the same tick. */
		ticks.add(() -> t += 0.15);

		running = true;
		scheduleTick();
	}

	/**
	 * Creates a Bio7 custom view for the given secondary id - one of the
	 * placeholders defined in com.eco.bio7.collection.CustomPerspective ("left",
	 * "right", "subLeftTop", "subLeftBottom", "subRightTop", "subRightBottom",
	 * "subRightBottomLeft", "subRightBottomRight") - and places the given Draw2d
	 * figure inside it.
	 */
	private <T extends IFigure> T show(String secId, String title, T figure) {
		CustomView view = new CustomView(title);
		LightweightSystem lws = view.getDraw2d(secId);
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
