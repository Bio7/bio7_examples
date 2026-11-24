package gui.web;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;

/*
 * WindowBuilder JavaScript example: the forest fire model (Drossel and Schwabl,
 * a cellular automaton) as an HTML page. This class is only the design (open it
 * with the WindowBuilder); the page is created from it with the context menu:
 * Create JavaScript App. The simulation is in web/ForestFireGui/app.js.
 *
 * The rules of a time step: a burning tree becomes an empty cell, a tree burns
 * if a neighbour burns or (with the probability f) by lightning, a tree grows on
 * an empty cell with the probability p.
 */
public class ForestFireGui extends Composite {

	private Scale scaleGrowth;
	private Scale scaleLightning;
	private Combo comboSize;
	private Spinner spinnerSpeed;
	private Composite cmpButtons;
	private Button btnStart;
	private Button btnStop;
	private Button btnReset;
	private Canvas canvasForest;
	private Canvas canvasSeries;
	private Label outStatus;

	public ForestFireGui(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));

		Label lblGrowth = new Label(this, SWT.NONE);
		lblGrowth.setText("Tree growth p (per 1000)");

		scaleGrowth = new Scale(this, SWT.NONE);
		scaleGrowth.setMinimum(1);
		scaleGrowth.setMaximum(100);
		scaleGrowth.setSelection(30);
		scaleGrowth.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblLightning = new Label(this, SWT.NONE);
		lblLightning.setText("Lightning f (per 100000)");

		scaleLightning = new Scale(this, SWT.NONE);
		scaleLightning.setMinimum(1);
		scaleLightning.setMaximum(100);
		scaleLightning.setSelection(5);
		scaleLightning.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSize = new Label(this, SWT.NONE);
		lblSize.setText("Grid size");

		comboSize = new Combo(this, SWT.READ_ONLY);
		comboSize.setItems(new String[] { "100", "150", "200", "300" });
		comboSize.select(2);
		comboSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSpeed = new Label(this, SWT.NONE);
		lblSpeed.setText("Steps per frame");

		spinnerSpeed = new Spinner(this, SWT.BORDER);
		spinnerSpeed.setMinimum(1);
		spinnerSpeed.setMaximum(20);
		spinnerSpeed.setSelection(1);
		spinnerSpeed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		cmpButtons = new Composite(this, SWT.NONE);
		cmpButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		cmpButtons.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		btnStart = new Button(cmpButtons, SWT.NONE);
		btnStart.setText("Start");

		btnStop = new Button(cmpButtons, SWT.NONE);
		btnStop.setText("Stop");

		btnReset = new Button(cmpButtons, SWT.NONE);
		btnReset.setText("Reset");

		canvasForest = new Canvas(this, SWT.BORDER);
		GridData gdForest = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdForest.widthHint = 600;
		gdForest.heightHint = 600;
		canvasForest.setLayoutData(gdForest);

		canvasSeries = new Canvas(this, SWT.BORDER);
		GridData gdSeries = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gdSeries.widthHint = 600;
		gdSeries.heightHint = 150;
		canvasSeries.setLayoutData(gdSeries);

		outStatus = new Label(this, SWT.NONE);
		outStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		outStatus.setText("(the status)");
	}
}
