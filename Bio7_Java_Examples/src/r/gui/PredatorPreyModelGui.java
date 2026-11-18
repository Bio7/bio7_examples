package r.gui;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;

/*
 * The GUI of PredatorPreyModel (editable with the WindowBuilder). Each slider
 * has a label with its value; the values are mapped from the integer
 * positions of the sliders in value().
 */
public class PredatorPreyModelGui extends Composite {

	private PredatorPreyModel model;
	private Scale scaleR;
	private Scale scaleK;
	private Scale scaleA;
	private Scale scaleB;
	private Scale scaleM;
	private Scale scaleN0;
	private Scale scaleP0;
	private Scale scaleYears;
	private Label lblRValue;
	private Label lblKValue;
	private Label lblAValue;
	private Label lblBValue;
	private Label lblMValue;
	private Label lblN0Value;
	private Label lblP0Value;
	private Label lblYearsValue;
	private Button btnLive;
	private Label lblResult;

	public PredatorPreyModelGui(Composite parent, PredatorPreyModel model, int style) {
		super(parent, style);
		this.model = model;
		setLayout(new GridLayout(3, false));

		SelectionAdapter changed = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLabels();
				if (btnLive.getSelection()) {
					run();
				}
			}
		};

		Label lblR = new Label(this, SWT.NONE);
		lblR.setText("Growth of the prey r:");
		scaleR = new Scale(this, SWT.NONE);
		scaleR.setMaximum(300);
		scaleR.setSelection(100);
		scaleR.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scaleR.addSelectionListener(changed);
		lblRValue = new Label(this, SWT.NONE);
		lblRValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

		Label lblK = new Label(this, SWT.NONE);
		lblK.setText("Carrying capacity K:");
		scaleK = new Scale(this, SWT.NONE);
		scaleK.setMinimum(10);
		scaleK.setMaximum(1000);
		scaleK.setSelection(500);
		scaleK.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scaleK.addSelectionListener(changed);
		lblKValue = new Label(this, SWT.NONE);

		Label lblA = new Label(this, SWT.NONE);
		lblA.setText("Attack rate a:");
		scaleA = new Scale(this, SWT.NONE);
		scaleA.setMinimum(1);
		scaleA.setMaximum(100);
		scaleA.setSelection(20);
		scaleA.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scaleA.addSelectionListener(changed);
		lblAValue = new Label(this, SWT.NONE);

		Label lblB = new Label(this, SWT.NONE);
		lblB.setText("Conversion efficiency b:");
		scaleB = new Scale(this, SWT.NONE);
		scaleB.setMinimum(1);
		scaleB.setMaximum(100);
		scaleB.setSelection(30);
		scaleB.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scaleB.addSelectionListener(changed);
		lblBValue = new Label(this, SWT.NONE);

		Label lblM = new Label(this, SWT.NONE);
		lblM.setText("Mortality of the predators m:");
		scaleM = new Scale(this, SWT.NONE);
		scaleM.setMinimum(1);
		scaleM.setMaximum(200);
		scaleM.setSelection(40);
		scaleM.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scaleM.addSelectionListener(changed);
		lblMValue = new Label(this, SWT.NONE);

		Label lblN0 = new Label(this, SWT.NONE);
		lblN0.setText("Prey at the start:");
		scaleN0 = new Scale(this, SWT.NONE);
		scaleN0.setMinimum(1);
		scaleN0.setMaximum(500);
		scaleN0.setSelection(40);
		scaleN0.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scaleN0.addSelectionListener(changed);
		lblN0Value = new Label(this, SWT.NONE);

		Label lblP0 = new Label(this, SWT.NONE);
		lblP0.setText("Predators at the start:");
		scaleP0 = new Scale(this, SWT.NONE);
		scaleP0.setMinimum(1);
		scaleP0.setMaximum(200);
		scaleP0.setSelection(9);
		scaleP0.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scaleP0.addSelectionListener(changed);
		lblP0Value = new Label(this, SWT.NONE);

		Label lblYears = new Label(this, SWT.NONE);
		lblYears.setText("Years:");
		scaleYears = new Scale(this, SWT.NONE);
		scaleYears.setMinimum(10);
		scaleYears.setMaximum(300);
		scaleYears.setSelection(100);
		scaleYears.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		scaleYears.addSelectionListener(changed);
		lblYearsValue = new Label(this, SWT.NONE);

		btnLive = new Button(this, SWT.CHECK);
		btnLive.setSelection(true);
		btnLive.setText("Live");

		Button btnRun = new Button(this, SWT.NONE);
		btnRun.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				run();
			}
		});
		btnRun.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		btnRun.setText("Run the model in R");

		lblResult = new Label(this, SWT.WRAP);
		lblResult.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		lblResult.setText("Move a slider or press Run (the plots are in the R plot view).");

		updateLabels();
	}

	/* The parameters: r, K, a, b, m, N0, P0, years */
	private double[] values() {
		return new double[] { scaleR.getSelection() / 100.0, scaleK.getSelection(), scaleA.getSelection() / 1000.0, scaleB.getSelection() / 100.0,
				scaleM.getSelection() / 100.0, scaleN0.getSelection(), scaleP0.getSelection(), scaleYears.getSelection() };
	}

	private void updateLabels() {
		double[] v = values();
		Label[] labels = { lblRValue, lblKValue, lblAValue, lblBValue, lblMValue, lblN0Value, lblP0Value, lblYearsValue };
		String[] formats = { "%.2f", "%.0f", "%.3f", "%.2f", "%.2f", "%.0f", "%.0f", "%.0f" };
		for (int i = 0; i < labels.length; i++) {
			labels[i].setText(String.format(Locale.ROOT, formats[i], v[i]) + "      ");
		}
		layout();
	}

	private void run() {
		model.simulate(values());
	}

	public void setResult(String text) {
		lblResult.setText(text);
	}
}
