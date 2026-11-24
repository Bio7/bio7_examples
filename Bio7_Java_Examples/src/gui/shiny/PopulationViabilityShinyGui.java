package gui.shiny;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;

/*
 * WindowBuilder Shiny example: a population viability analysis (stochastic
 * births and deaths, Monte Carlo) as a Shiny app. This class is only the design
 * (open it with the WindowBuilder); the app is created from it with the context
 * menu: Create Shiny App (WindowBuilder Shiny). The simulation is in
 * shiny/PopulationViabilityShinyGui/server.R.
 *
 * Each year every individual gets Poisson(b) offspring (the birth rate falls
 * with the density: b (1 - N / K)) and dies with the probability d; a
 * catastrophe kills half of the population with its probability. Many runs
 * give the risk of extinction.
 */
public class PopulationViabilityShinyGui extends Composite {

	private Scale scaleBirth;
	private Scale scaleDeath;
	private Scale scaleCatastrophe;
	private Spinner spinnerK;
	private Spinner spinnerN0;
	private Spinner spinnerYears;
	private Spinner spinnerRuns;
	private Group grpScale;
	private Button rbLinear;
	private Button rbLogarithmic;
	private Composite cmpButtons;
	private Button btnRun;
	private Canvas canvasRuns;
	private Canvas canvasRisk;
	private Label outSummary;
	private Table tableQuantiles;

	public PopulationViabilityShinyGui(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(4, false));

		Label lblBirth = new Label(this, SWT.NONE);
		lblBirth.setText("Birth rate (1/100)");

		scaleBirth = new Scale(this, SWT.NONE);
		scaleBirth.setMinimum(5);
		scaleBirth.setMaximum(100);
		scaleBirth.setSelection(40);
		scaleBirth.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblDeath = new Label(this, SWT.NONE);
		lblDeath.setText("Death rate (1/100)");

		scaleDeath = new Scale(this, SWT.NONE);
		scaleDeath.setMinimum(5);
		scaleDeath.setMaximum(100);
		scaleDeath.setSelection(30);
		scaleDeath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblCatastrophe = new Label(this, SWT.NONE);
		lblCatastrophe.setText("Catastrophes (1/100 per year)");

		scaleCatastrophe = new Scale(this, SWT.NONE);
		scaleCatastrophe.setMaximum(20);
		scaleCatastrophe.setSelection(2);
		scaleCatastrophe.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblK = new Label(this, SWT.NONE);
		lblK.setText("Carrying capacity");

		spinnerK = new Spinner(this, SWT.BORDER);
		spinnerK.setMinimum(10);
		spinnerK.setMaximum(5000);
		spinnerK.setSelection(100);
		spinnerK.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblN0 = new Label(this, SWT.NONE);
		lblN0.setText("Population at the start");

		spinnerN0 = new Spinner(this, SWT.BORDER);
		spinnerN0.setMinimum(1);
		spinnerN0.setMaximum(5000);
		spinnerN0.setSelection(20);
		spinnerN0.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblYears = new Label(this, SWT.NONE);
		lblYears.setText("Years");

		spinnerYears = new Spinner(this, SWT.BORDER);
		spinnerYears.setMinimum(10);
		spinnerYears.setMaximum(1000);
		spinnerYears.setSelection(100);
		spinnerYears.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblRuns = new Label(this, SWT.NONE);
		lblRuns.setText("Runs");

		spinnerRuns = new Spinner(this, SWT.BORDER);
		spinnerRuns.setMinimum(10);
		spinnerRuns.setMaximum(5000);
		spinnerRuns.setSelection(200);
		spinnerRuns.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		grpScale = new Group(this, SWT.NONE);
		grpScale.setText("Axis of the population");
		grpScale.setLayout(new RowLayout(SWT.HORIZONTAL));
		grpScale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		rbLinear = new Button(grpScale, SWT.RADIO);
		rbLinear.setSelection(true);
		rbLinear.setText("Linear");

		rbLogarithmic = new Button(grpScale, SWT.RADIO);
		rbLogarithmic.setText("Logarithmic");

		cmpButtons = new Composite(this, SWT.NONE);
		cmpButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		cmpButtons.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

		btnRun = new Button(cmpButtons, SWT.NONE);
		btnRun.setText("New runs");

		canvasRuns = new Canvas(this, SWT.BORDER);
		GridData gdRuns = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdRuns.heightHint = 350;
		canvasRuns.setLayoutData(gdRuns);

		canvasRisk = new Canvas(this, SWT.BORDER);
		GridData gdRisk = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdRisk.heightHint = 350;
		canvasRisk.setLayoutData(gdRisk);

		outSummary = new Label(this, SWT.NONE);
		outSummary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		outSummary.setText("(the summary)");

		tableQuantiles = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
		tableQuantiles.setHeaderVisible(true);
		tableQuantiles.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 4, 1));
	}
}
