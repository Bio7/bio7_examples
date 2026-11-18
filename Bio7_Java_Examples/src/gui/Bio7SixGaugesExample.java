package gui;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.datadefinition.IManualValueChangeListener;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.KnobFigure;
import org.eclipse.nebula.visualization.widgets.figures.MeterFigure;
import org.eclipse.nebula.visualization.widgets.figures.LinearScaleFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.nebula.visualization.widgets.figures.ThermometerFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import com.eco.bio7.collection.CustomView;

/**
 * Ein erweitertes Live-Update-Beispiel für Bio7 mit 6 verschiedenen Widgets.
 * Der Drehknopf (Knob) steuert die 5 anderen Anzeigeinstrumente synchron an.
 */
public class Bio7SixGaugesExample extends com.eco.bio7.compile.Model {
    
    @Override
    public void setup() {
        
        // 1. Bio7 CustomView initialisieren
        CustomView view = new CustomView();
        org.eclipse.swt.widgets.Composite parent = view.getComposite("Bio7SixGaugesExample");
        
        // 2. Layout für 6 Widgets festlegen (3 Spalten, 2 Zeilen)
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        parent.setLayout(layout);
        
        // 3. 6 Canvas-Bereiche als Container für Draw2D erstellen
        Canvas knobCanvas = new Canvas(parent, SWT.NONE);
        knobCanvas.setLayoutData(gd);
        Canvas gaugeCanvas = new Canvas(parent, SWT.NONE);
        gaugeCanvas.setLayoutData(gd);
        Canvas meterCanvas = new Canvas(parent, SWT.NONE);
        meterCanvas.setLayoutData(gd);
        Canvas thermoCanvas = new Canvas(parent, SWT.NONE);
        thermoCanvas.setLayoutData(gd);
        Canvas tankCanvas = new Canvas(parent, SWT.NONE);
        tankCanvas.setLayoutData(gd);
        Canvas scaleCanvas = new Canvas(parent, SWT.NONE);
        scaleCanvas.setLayoutData(gd);
        
        // 4. Brücke zwischen SWT und Draw2D (LightweightSystem) und Widgets initialisieren
        
        // 1. Knob (Drehknopf / Steuerelement)
        LightweightSystem lwsKnob = new LightweightSystem(knobCanvas);     
        final KnobFigure knobFigure = new KnobFigure();         
        lwsKnob.setContents(knobFigure);    
        
        // 2. Gauge (Klassische runde Messuhr)
        LightweightSystem lwsGauge = new LightweightSystem(gaugeCanvas);       
        final GaugeFigure gauge = new GaugeFigure();        
        gauge.setBackgroundColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
        gauge.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
        lwsGauge.setContents(gauge);    
        
        // 3. Meter (Halbkreisförmiges Anzeigeinstrument)
        LightweightSystem lwsMeter = new LightweightSystem(meterCanvas);
        final MeterFigure meter = new MeterFigure();
        lwsMeter.setContents(meter);
        
        // 4. Thermometer
        LightweightSystem lwsThermo = new LightweightSystem(thermoCanvas);      
        final ThermometerFigure thermo = new ThermometerFigure();           
        lwsThermo.setContents(thermo);  
        
        // 5. Tank (Füllstandsanzeige)
        LightweightSystem lwsTank = new LightweightSystem(tankCanvas);     
        final TankFigure tank = new TankFigure();           
        lwsTank.setContents(tank);
        
        // 6. Linear Scale (Horizontale/Vertikale lineare Skala)
        LightweightSystem lwsScale = new LightweightSystem(scaleCanvas);
        final LinearScaleFigure linearScale = new LinearScaleFigure();
        lwsScale.setContents(linearScale);
        
        // 5. Listener hinzufügen (Knob steuert die anderen 5 Widgets)
        knobFigure.addManualValueChangeListener(new IManualValueChangeListener() {          
            @Override
            public void manualValueChanged(double newValue) {
                gauge.setValue(newValue);
                meter.setValue(newValue);
                thermo.setValue(newValue);
                tank.setValue(newValue);
                linearScale.setValue(newValue);
            }
        }); 
        
        // 6. View in Bio7 neu rendern
        parent.layout();
    }
}
