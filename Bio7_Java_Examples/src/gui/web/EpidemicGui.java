package gui.web;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;

/*
 * WindowBuilder JavaScript example: an epidemic (the SIR model) as an HTML page.
 * This class is only the design (open it with the WindowBuilder); the page is
 * created from it with the context menu: Create JavaScript App. The simulation
 * is in web/EpidemicGui/app.js (JavaScript, runs in the browser of Bio7 or in
 * any browser).
 *
 *   dS/dt = -beta S I     dI/dt = beta S I - gamma I     dR/dt = gamma I
 *
 * The sliders: beta and gamma in 1/100 per day, the vaccinated part of the
 * population (in percent, immune from the start).
 */
public class EpidemicGui extends Composite {

	private Scale scaleBeta;
	private Scale scaleGamma;
	private Scale scaleVaccinated;
	private Spinner spinnerDays;
	private Button chkRecovered;
	private Composite cmpButtons;
	private Button btnRun;
	private Button btnAnimate;
	private Canvas canvasChart;
	private Label outSummary;
	private Table tableResults;

	public EpidemicGui(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));

		Label lblBeta = new Label(this, SWT.NONE);
		lblBeta.setText("Infection rate (1/100 per day)");

		scaleBeta = new Scale(this, SWT.NONE);
		scaleBeta.setMinimum(5);
		scaleBeta.setMaximum(100);
		scaleBeta.setSelection(30);
		scaleBeta.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblGamma = new Label(this, SWT.NONE);
		lblGamma.setText("Recovery rate (1/100 per day)");

		scaleGamma = new Scale(this, SWT.NONE);
		scaleGamma.setMinimum(1);
		scaleGamma.setMaximum(50);
		scaleGamma.setSelection(10);
		scaleGamma.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblVaccinated = new Label(this, SWT.NONE);
		lblVaccinated.setText("Vaccinated (%)");

		scaleVaccinated = new Scale(this, SWT.NONE);
		scaleVaccinated.setMaximum(95);
		scaleVaccinated.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblDays = new Label(this, SWT.NONE);
		lblDays.setText("Days");

		spinnerDays = new Spinner(this, SWT.BORDER);
		spinnerDays.setMinimum(10);
		spinnerDays.setMaximum(1000);
		spinnerDays.setSelection(160);
		spinnerDays.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		chkRecovered = new Button(this, SWT.CHECK);
		chkRecovered.setSelection(true);
		chkRecovered.setText("Show the recovered");

		cmpButtons = new Composite(this, SWT.NONE);
		cmpButtons.setLayout(new RowLayout(SWT.HORIZONTAL));

		btnRun = new Button(cmpButtons, SWT.NONE);
		btnRun.setText("Run");

		btnAnimate = new Button(cmpButtons, SWT.NONE);
		btnAnimate.setText("Animate");

		canvasChart = new Canvas(this, SWT.BORDER);
		GridData gdChart = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdChart.widthHint = 700;
		gdChart.heightHint = 350;
		canvasChart.setLayoutData(gdChart);

		outSummary = new Label(this, SWT.NONE);
		outSummary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		outSummary.setText("(the summary)");

		tableResults = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
		tableResults.setHeaderVisible(true);
		tableResults.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
	}
}
