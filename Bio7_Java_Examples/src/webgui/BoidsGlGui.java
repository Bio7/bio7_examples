package webgui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;

/*
 * WindowBuilder JavaScript example with WebGL: a flock of boids in 3D (Reynolds:
 * separation, alignment, cohesion) as an HTML page. This class is only the design
 * (open it with the WindowBuilder); the page is created from it with the context
 * menu: WindowBuilder Apps > Create JavaScript App. The simulation and the WebGL
 * drawing are in web/BoidsGlGui/app.js.
 *
 * A GLCanvas of SWT (or a Canvas whose name starts with "gl") becomes a canvas
 * with a WebGL context (in a Shiny app: rglwidgetOutput of the R package rgl).
 */
public class BoidsGlGui extends Composite {

	private Scale scaleBoids;
	private Scale scaleSeparation;
	private Scale scaleAlignment;
	private Scale scaleCohesion;
	private Scale scaleSpeed;
	private Button chkRotate;
	private Composite cmpButtons;
	private Button btnStart;
	private Button btnStop;
	private Button btnReset;
	private org.eclipse.swt.widgets.Canvas glView;
	private Label outStatus;

	public BoidsGlGui(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));

		Label lblBoids = new Label(this, SWT.NONE);
		lblBoids.setText("Boids");

		scaleBoids = new Scale(this, SWT.NONE);
		scaleBoids.setMinimum(50);
		scaleBoids.setMaximum(800);
		scaleBoids.setIncrement(50);
		scaleBoids.setSelection(300);
		scaleBoids.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSeparation = new Label(this, SWT.NONE);
		lblSeparation.setText("Separation");

		scaleSeparation = new Scale(this, SWT.NONE);
		scaleSeparation.setSelection(50);
		scaleSeparation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblAlignment = new Label(this, SWT.NONE);
		lblAlignment.setText("Alignment");

		scaleAlignment = new Scale(this, SWT.NONE);
		scaleAlignment.setSelection(50);
		scaleAlignment.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblCohesion = new Label(this, SWT.NONE);
		lblCohesion.setText("Cohesion");

		scaleCohesion = new Scale(this, SWT.NONE);
		scaleCohesion.setSelection(30);
		scaleCohesion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSpeed = new Label(this, SWT.NONE);
		lblSpeed.setText("Speed");

		scaleSpeed = new Scale(this, SWT.NONE);
		scaleSpeed.setMinimum(1);
		scaleSpeed.setMaximum(20);
		scaleSpeed.setSelection(6);
		scaleSpeed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		chkRotate = new Button(this, SWT.CHECK);
		chkRotate.setSelection(true);
		chkRotate.setText("Rotate the view");

		cmpButtons = new Composite(this, SWT.NONE);
		cmpButtons.setLayout(new RowLayout(SWT.HORIZONTAL));

		btnStart = new Button(cmpButtons, SWT.NONE);
		btnStart.setText("Start");

		btnStop = new Button(cmpButtons, SWT.NONE);
		btnStop.setText("Stop");

		btnReset = new Button(cmpButtons, SWT.NONE);
		btnReset.setText("Reset");

		glView = new org.eclipse.swt.widgets.Canvas(this, SWT.NONE);
		GridData gdView = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdView.widthHint = 700;
		gdView.heightHint = 500;
		glView.setLayoutData(gdView);

		outStatus = new Label(this, SWT.NONE);
		outStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		outStatus.setText("(the status)");
	}
}
