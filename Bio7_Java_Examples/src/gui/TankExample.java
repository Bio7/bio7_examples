package gui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.SchemeBorder;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.collection.CustomView;

/**
 * A live updated tank Example.
 * 
 * @author Xihui Chen
 *
 */
public class TankExample extends com.eco.bio7.compile.Model {
	int counter = 0;

	public void setup() {

		// use LightweightSystem to create the bridge between SWT and draw2D
		CustomView view = new CustomView();
		LightweightSystem lws = view.getDraw2d("TankExample");

		// Create widget
		final TankFigure tank = new TankFigure();

		// Init widget
		tank.setBackgroundColor(XYGraphMediaFactory.getInstance().getColor(255,
				255, 255));

		tank.setBorder(new SchemeBorder(SchemeBorder.SCHEMES.ETCHED));

		tank.setRange(-100, 100);
		tank.setLoLevel(-50);
		tank.setLoloLevel(-80);
		tank.setHiLevel(60);
		tank.setHihiLevel(80);
		tank.setMajorTickMarkStepHint(50);

		lws.setContents(tank);

		// Update the widget in another thread.
		ScheduledExecutorService scheduler = Executors
				.newScheduledThreadPool(1);
		ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
				new Runnable() {

					public void run() {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								tank.setValue(Math.sin(counter++ / 10.0) * 100);
							}
						});
					}
				}, 100, 100, TimeUnit.MILLISECONDS);

	}
}