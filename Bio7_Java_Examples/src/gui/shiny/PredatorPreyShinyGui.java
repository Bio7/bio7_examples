package gui.shiny;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;

/*
 * WindowBuilder Shiny example: a predator-prey model (Lotka-Volterra with a
 * carrying capacity of the prey) as a Shiny app. This class is only the design
 * (open it with the WindowBuilder); the app is created from it with the context
 * menu: Create Shiny App (WindowBuilder Shiny). The model is in
 * shiny/PredatorPreyShinyGui/server.R (R, solved with Runge-Kutta 4).
 *
 *   dN/dt = r N (1 - N / K) - a N P
 *   dP/dt = b a N P - m P
 *
 * The rates of the sliders are in 1/100 (the conversion is in server.R).
 */
public class PredatorPreyShinyGui extends Composite {

	private Group grpPrey;
	private Scale scaleR;
	private Scale scaleK;
	private Spinner spinnerN0;
	private Group grpPredator;
	private Scale scaleA;
	private Scale scaleB;
	private Scale scaleM;
	private Spinner spinnerP0;
	private Spinner spinnerYears;
	private Button chkEquilibrium;
	private TabFolder tabFolder;
	private TabItem tbtmSeries;
	private Canvas canvasSeries;
	private TabItem tbtmPhase;
	private Canvas canvasPhase;
	private Label outSummary;
	private Table tableEquilibrium;

	public PredatorPreyShinyGui(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, true));

		grpPrey = new Group(this, SWT.NONE);
		grpPrey.setText("Prey");
		grpPrey.setLayout(new GridLayout(2, false));
		grpPrey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		Label lblR = new Label(grpPrey, SWT.NONE);
		lblR.setText("Growth rate r");

		scaleR = new Scale(grpPrey, SWT.NONE);
		scaleR.setMinimum(10);
		scaleR.setMaximum(200);
		scaleR.setSelection(100);
		scaleR.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblK = new Label(grpPrey, SWT.NONE);
		lblK.setText("Carrying capacity K");

		scaleK = new Scale(grpPrey, SWT.NONE);
		scaleK.setMinimum(50);
		scaleK.setMaximum(1000);
		scaleK.setIncrement(10);
		scaleK.setSelection(500);
		scaleK.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblN0 = new Label(grpPrey, SWT.NONE);
		lblN0.setText("Prey at the start");

		spinnerN0 = new Spinner(grpPrey, SWT.BORDER);
		spinnerN0.setMinimum(1);
		spinnerN0.setMaximum(1000);
		spinnerN0.setSelection(100);
		spinnerN0.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		grpPredator = new Group(this, SWT.NONE);
		grpPredator.setText("Predator");
		grpPredator.setLayout(new GridLayout(2, false));
		grpPredator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		Label lblA = new Label(grpPredator, SWT.NONE);
		lblA.setText("Attack rate a (1/1000)");

		scaleA = new Scale(grpPredator, SWT.NONE);
		scaleA.setMinimum(1);
		scaleA.setMaximum(50);
		scaleA.setSelection(10);
		scaleA.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblB = new Label(grpPredator, SWT.NONE);
		lblB.setText("Conversion b");

		scaleB = new Scale(grpPredator, SWT.NONE);
		scaleB.setMinimum(5);
		scaleB.setMaximum(100);
		scaleB.setSelection(50);
		scaleB.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblM = new Label(grpPredator, SWT.NONE);
		lblM.setText("Mortality m");

		scaleM = new Scale(grpPredator, SWT.NONE);
		scaleM.setMinimum(5);
		scaleM.setMaximum(100);
		scaleM.setSelection(40);
		scaleM.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblP0 = new Label(grpPredator, SWT.NONE);
		lblP0.setText("Predators at the start");

		spinnerP0 = new Spinner(grpPredator, SWT.BORDER);
		spinnerP0.setMinimum(1);
		spinnerP0.setMaximum(500);
		spinnerP0.setSelection(20);
		spinnerP0.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblYears = new Label(this, SWT.NONE);
		lblYears.setText("Years");

		spinnerYears = new Spinner(this, SWT.BORDER);
		spinnerYears.setMinimum(10);
		spinnerYears.setMaximum(500);
		spinnerYears.setSelection(100);
		spinnerYears.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		chkEquilibrium = new Button(this, SWT.CHECK);
		chkEquilibrium.setSelection(true);
		chkEquilibrium.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		chkEquilibrium.setText("Show the equilibrium");

		tabFolder = new TabFolder(this, SWT.NONE);
		GridData gdTabs = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdTabs.heightHint = 400;
		tabFolder.setLayoutData(gdTabs);

		tbtmSeries = new TabItem(tabFolder, SWT.NONE);
		tbtmSeries.setText("Time series");

		canvasSeries = new Canvas(tabFolder, SWT.NONE);
		tbtmSeries.setControl(canvasSeries);

		tbtmPhase = new TabItem(tabFolder, SWT.NONE);
		tbtmPhase.setText("Phase plane");

		canvasPhase = new Canvas(tabFolder, SWT.NONE);
		tbtmPhase.setControl(canvasPhase);

		outSummary = new Label(this, SWT.NONE);
		outSummary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		outSummary.setText("(the summary)");

		tableEquilibrium = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
		tableEquilibrium.setHeaderVisible(true);
		tableEquilibrium.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
	}
}
