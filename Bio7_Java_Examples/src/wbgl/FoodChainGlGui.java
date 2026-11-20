package wbgl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;

/*
 * WebGL and Shiny 3D from one WindowBuilder design: a food chain of three species
 * (Hastings and Powell 1991). Its dynamics are a cycle or chaos (the "teacup"
 * attractor), depending on the half saturation b1 of the predator.
 *
 *   dx/dt = x (1 - x) - a1 x y / (1 + b1 x)                          prey
 *   dy/dt = a1 x y / (1 + b1 x) - a2 y z / (1 + b2 y) - d1 y          predator
 *   dz/dt = a2 y z / (1 + b2 y) - d2 z                                top predator
 *
 * The canvas glAttractor is a normal Canvas of the WindowBuilder: its name starts
 * with "gl" (and an uppercase letter), so it becomes a 3D view - in the Shiny app
 * rglwidgetOutput (the R package rgl), in the JavaScript app a canvas with WebGL.
 * The canvas canvasSeries is a normal plot (plotOutput) or a 2D canvas.
 *
 * Context menu of this file > WindowBuilder Apps: Create Shiny App
 * (shiny/FoodChainGlGui) or Create JavaScript App (web/FoodChainGlGui).
 */
public class FoodChainGlGui extends Composite {

	private Scale scaleB1;
	private Spinner spinnerTime;
	private Scale scaleTransient;
	private Combo comboColor;
	private Button btnRun;
	private Canvas glAttractor;
	private Canvas canvasSeries;
	private Label outSummary;

	public FoodChainGlGui(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));

		Label lblB1 = new Label(this, SWT.NONE);
		lblB1.setText("Half saturation b1 (1/10)");

		scaleB1 = new Scale(this, SWT.NONE);
		scaleB1.setMinimum(20);
		scaleB1.setMaximum(62);
		scaleB1.setSelection(30);
		scaleB1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblTime = new Label(this, SWT.NONE);
		lblTime.setText("Time");

		spinnerTime = new Spinner(this, SWT.BORDER);
		spinnerTime.setMinimum(500);
		spinnerTime.setMaximum(20000);
		spinnerTime.setIncrement(500);
		spinnerTime.setSelection(5000);
		spinnerTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblTransient = new Label(this, SWT.NONE);
		lblTransient.setText("Transient left out (%)");

		scaleTransient = new Scale(this, SWT.NONE);
		scaleTransient.setMaximum(80);
		scaleTransient.setSelection(20);
		scaleTransient.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblColor = new Label(this, SWT.NONE);
		lblColor.setText("Color of the curve");

		comboColor = new Combo(this, SWT.READ_ONLY);
		comboColor.setItems(new String[] { "Time", "Top predator" });
		comboColor.select(0);
		comboColor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		btnRun = new Button(this, SWT.NONE);
		btnRun.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnRun.setText("Run");

		glAttractor = new Canvas(this, SWT.BORDER);
		GridData gdAttractor = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gdAttractor.widthHint = 500;
		gdAttractor.heightHint = 450;
		glAttractor.setLayoutData(gdAttractor);

		canvasSeries = new Canvas(this, SWT.BORDER);
		GridData gdSeries = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gdSeries.widthHint = 500;
		gdSeries.heightHint = 450;
		canvasSeries.setLayoutData(gdSeries);

		outSummary = new Label(this, SWT.WRAP);
		outSummary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		outSummary.setText("(the summary)");
	}
}
