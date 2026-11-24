package gui.swt;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.datadefinition.IManualValueChangeListener;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.KnobFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.nebula.visualization.widgets.figures.ThermometerFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import com.eco.bio7.collection.CustomView;

/**
 * Ein live aktualisiertes Gauge-Beispiel für Bio7.
 * Integriert Nebula Visualisierungs-Widgets in eine CustomView.
 */
public class Bio7GaugeExample extends com.eco.bio7.compile.Model {
    
    @Override
    public void setup() {
        
        // 1. Bio7 CustomView initialisieren und das Composite-Parent abgreifen
        CustomView view = new CustomView();
        org.eclipse.swt.widgets.Composite parent = view.getComposite("Bio7GaugeExample");
        
        // 2. Layout für die Ansicht festlegen (2 Spalten für 4 Widgets)
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        parent.setLayout(layout);
        
        // 3. Canvas-Bereiche als Container für Draw2D erstellen
        Canvas knobCanvas = new Canvas(parent, SWT.NONE);
        knobCanvas.setLayoutData(gd);
        Canvas gaugeCanvas = new Canvas(parent, SWT.NONE);
        gaugeCanvas.setLayoutData(gd);
        Canvas thermoCanvas = new Canvas(parent, SWT.NONE);
        thermoCanvas.setLayoutData(gd);
        Canvas tankCanvas = new Canvas(parent, SWT.NONE);
        tankCanvas.setLayoutData(gd);
        
        // 4. Brücke zwischen SWT und Draw2D über LightweightSystem schlagen und Widgets erstellen
        
        // Knob (Drehknopf)
        LightweightSystem lwsKnob = new LightweightSystem(knobCanvas);     
        final KnobFigure knobFigure = new KnobFigure();         
        lwsKnob.setContents(knobFigure);    
        
        // Gauge (Messuhr mit schwarzem Hintergrund)
        LightweightSystem lwsGauge = new LightweightSystem(gaugeCanvas);       
        final GaugeFigure gauge = new GaugeFigure();        
        gauge.setBackgroundColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
        gauge.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
        lwsGauge.setContents(gauge);    
        
        // Thermometer
        LightweightSystem lwsThermo = new LightweightSystem(thermoCanvas);      
        final ThermometerFigure thermo = new ThermometerFigure();           
        lwsThermo.setContents(thermo);  
        
        // Tank (Füllstandsanzeige)
        LightweightSystem lwsTank = new LightweightSystem(tankCanvas);     
        final TankFigure tank = new TankFigure();           
        lwsTank.setContents(tank);
        
        // 5. Listener hinzufügen (Änderungen am Drehknopf aktualisieren alle anderen Anzeigen)
        knobFigure.addManualValueChangeListener(new IManualValueChangeListener() {          
            @Override
            public void manualValueChanged(double newValue) {
                gauge.setValue(newValue);
                thermo.setValue(newValue);
                tank.setValue(newValue);
            }
        }); 
        
        // 6. Layout-Methode der Bio7-View aufrufen, um die UI zu rendern
        parent.layout();
    }
}
