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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.KnobFigure;
import org.eclipse.nebula.visualization.widgets.figures.MeterFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.collection.Work;

/**
 * 4 custom views arranged as a true 2 x 2 quadrant grid in the built-in
 * "Custom" perspective (com.eco.bio7.collection.CustomPerspective), each
 * occupying exactly 50% of the perspective's width and 50% of its height:
 *
 * <pre>
 * +------------------------+------------------------+
 * |   Top Left (Speed)     |   Top Right (Temp)     |
 * |         50% x 50%      |        50% x 50%       |
 * +------------------------+------------------------+
 * |  Bottom Left (Volume)  |  Bottom Right (Fuel)   |
 * |         50% x 50%      |        50% x 50%       |
 * +------------------------+------------------------+
 * </pre>
 *
 * The perspective's "MainLeft"/"MainRight" columns are each 50% wide and its
 * "subLeftTop"/"subLeftBottom"/"subRightTop"/"subRightBottom" folders each
 * split their column 50/50 top to bottom, so these 4 secondary view ids are
 * exactly the 4 quadrants used here.
 */
public class FourQuadrantExample extends com.eco.bio7.compile.Model {

	private static final int UPDATE_INTERVAL = 150;

	private final List<Runnable> ticks = new ArrayList<Runnable>();

	private volatile boolean running = false;

	private double t = 0;

	public void setup() {

		/* Switch to the perspective which provides the 4 quadrant placeholders. */
		Work.openPerspective("com.eco.bio7.CustomPerspective");

		/* Top left quadrant: speed gauge. */
		final GaugeFigure speed = show("subLeftTop", "Speed", new GaugeFigure());
		speed.setUnit("km/h");
		speed.setRange(0, 220);
		speed.setHiLevel(160);
		speed.setShowHi(true);
		speed.setHiColor(ColorConstants.orange);
		speed.setHihiLevel(200);
		speed.setShowHihi(true);
		speed.setHihiColor(ColorConstants.red);
		ticks.add(() -> speed.setValue(110 + 90 * Math.sin(t * 0.6)));

		/* Top right quadrant: temperature meter. */
		final MeterFigure temperature = show("subRightTop", "Temperature", new MeterFigure());
		temperature.setRange(-20, 50);
		temperature.setHiLevel(35);
		temperature.setShowHi(true);
		temperature.setHiColor(ColorConstants.orange);
		ticks.add(() -> temperature.setValue(15 + 20 * Math.sin(t * 0.35 + 1)));

		/* Bottom left quadrant: volume knob. */
		final KnobFigure volume = show("subLeftBottom", "Volume", new KnobFigure());
		volume.setRange(0, 100);
		ticks.add(() -> volume.manualSetValue(50 + 45 * Math.sin(t * 0.9 + 2)));

		/* Bottom right quadrant: fuel tank. */
		final TankFigure fuel = show("subRightBottom", "Fuel Level", new TankFigure());
		fuel.setRange(0, 100);
		fuel.setLoLevel(20);
		fuel.setShowLo(true);
		fuel.setLoColor(ColorConstants.orange);
		fuel.setFillColor(ColorConstants.lightBlue);
		ticks.add(() -> fuel.setValue(50 - 20 * Math.sin(t * 0.05)));

		/* Advance the shared clock last so every widget uses the same tick. */
		ticks.add(() -> t += 0.15);

		running = true;
		scheduleTick();
	}

	/**
	 * Creates a Bio7 custom view for the given secondary id - one of the 4
	 * equal-sized quadrant placeholders defined in
	 * com.eco.bio7.collection.CustomPerspective - and places the given Draw2d
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
