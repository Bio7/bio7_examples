package gui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.SchemeBorder;
import org.eclipse.nebula.visualization.widgets.figures.MeterFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.collection.CustomView;


/**
 * A live updated Gauge Example.
 * @author Xihui Chen
 *
 * Source adapted for Bio7.
 */
public class MeterExample extends com.eco.bio7.compile.Model{
	
	private static int counter = 0;
	public void setup() { 
		
	    
	 // use LightweightSystem to create the bridge between SWT and draw2D
	 		CustomView view = new CustomView();
	 		LightweightSystem lws = view.getDraw2d("MeterExample");
		
		//Create Gauge
		final MeterFigure meterFigure = new MeterFigure();
		
		//Init gauge
		meterFigure.setBackgroundColor(
				XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		
		meterFigure.setBorder(new SchemeBorder(SchemeBorder.SCHEMES.ETCHED));
		
		meterFigure.setRange(-100, 100);
		meterFigure.setLoLevel(-50);
		meterFigure.setLoloLevel(-80);
		meterFigure.setHiLevel(60);
		meterFigure.setHihiLevel(80);
		meterFigure.setMajorTickMarkStepHint(50);
		
		lws.setContents(meterFigure);		
		
		//Update the gauge in another thread.
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new Runnable() {
			
			public void run() {
				Display.getDefault().asyncExec(new Runnable() {					
					public void run() {
						meterFigure.setValue(Math.sin(counter++/10.0)*100);						
					}
				});
			}
		}, 100, 100, TimeUnit.MILLISECONDS);		
		
	   
	   
	   
	}
}