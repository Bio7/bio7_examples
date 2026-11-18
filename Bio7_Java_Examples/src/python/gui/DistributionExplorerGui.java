package python.gui;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;

/*
 * The GUI of DistributionExplorer (editable with the WindowBuilder).
 */
public class DistributionExplorerGui extends Composite {

	private DistributionExplorer model;
	private Combo comboDistribution;
	private Spinner spinnerN;
	private Scale scaleP1;
	private Scale scaleP2;
	private Label lblP1;
	private Label lblP2;
	private Label lblP1Value;
	private Label lblP2Value;
	private Spinner spinnerBins;
	private Button btnLive;
	private Label lblSummary;

	public DistributionExplorerGui(Composite parent, DistributionExplorer model, int style) {
		super(parent, style);
		this.model = model;
		setLayout(new GridLayout(3, false));

		Label lblDistribution = new Label(this, SWT.NONE);
		lblDistribution.setText("Distribution:");

		comboDistribution = new Combo(this, SWT.READ_ONLY);
		comboDistribution.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLabels();
				changed();
			}
		});
		comboDistribution.setItems(new String[] { "normal", "lognormal", "gamma", "exponential", "poisson" });
		comboDistribution.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		comboDistribution.select(0);

		Label lblN = new Label(this, SWT.NONE);
		lblN.setText("Sample size:");

		spinnerN = new Spinner(this, SWT.BORDER);
		spinnerN.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changed();
			}
		});
		spinnerN.setIncrement(50);
		spinnerN.setMaximum(100000);
		spinnerN.setMinimum(10);
		spinnerN.setSelection(500);
		spinnerN.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		lblP1 = new Label(this, SWT.NONE);
		lblP1.setText("Mean:");

		scaleP1 = new Scale(this, SWT.NONE);
		scaleP1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLabels();
				changed();
			}
		});
		scaleP1.setMaximum(100);
		scaleP1.setMinimum(1);
		scaleP1.setSelection(20);
		scaleP1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		lblP1Value = new Label(this, SWT.NONE);
		GridData gdP1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gdP1.widthHint = 50;
		lblP1Value.setLayoutData(gdP1);

		lblP2 = new Label(this, SWT.NONE);
		lblP2.setText("SD:");

		scaleP2 = new Scale(this, SWT.NONE);
		scaleP2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLabels();
				changed();
			}
		});
		scaleP2.setMaximum(100);
		scaleP2.setMinimum(1);
		scaleP2.setSelection(10);
		scaleP2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		lblP2Value = new Label(this, SWT.NONE);
		GridData gdP2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gdP2.widthHint = 50;
		lblP2Value.setLayoutData(gdP2);

		Label lblBins = new Label(this, SWT.NONE);
		lblBins.setText("Bins:");

		spinnerBins = new Spinner(this, SWT.BORDER);
		spinnerBins.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changed();
			}
		});
		spinnerBins.setMaximum(200);
		spinnerBins.setMinimum(3);
		spinnerBins.setSelection(30);
		spinnerBins.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		btnLive = new Button(this, SWT.CHECK);
		btnLive.setSelection(true);
		btnLive.setText("Live (redraw at each change)");

		Button btnPlot = new Button(this, SWT.NONE);
		btnPlot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				plot();
			}
		});
		btnPlot.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		btnPlot.setText("Plot");

		lblSummary = new Label(this, SWT.WRAP);
		lblSummary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		lblSummary.setText("Press Plot (the plot is in the Python Plot view).");

		updateLabels();
	}

	/* The parameters of the distribution (the scale values 1..100 mapped) */
	private double p1() {
		return switch (comboDistribution.getText()) {
		case "normal" -> scaleP1.getSelection() / 2.0 - 10;   // mean -9.5 .. 40
		case "lognormal", "exponential", "poisson" -> scaleP1.getSelection() / 4.0;   // median, mean, lambda 0.25 .. 25
		default -> scaleP1.getSelection() / 10.0;              // gamma shape 0.1 .. 10
		};
	}

	private double p2() {
		return switch (comboDistribution.getText()) {
		case "lognormal" -> scaleP2.getSelection() / 50.0;     // sigma of the log 0.02 .. 2
		default -> scaleP2.getSelection() / 5.0;               // sd, gamma scale 0.2 .. 20
		};
	}

	private void updateLabels() {
		String d = comboDistribution.getText();
		String[] names = switch (d) {
		case "normal" -> new String[] { "Mean:", "SD:" };
		case "lognormal" -> new String[] { "Median:", "Sigma (log):" };
		case "gamma" -> new String[] { "Shape:", "Scale:" };
		case "exponential" -> new String[] { "Mean:", "(unused)" };
		default -> new String[] { "Lambda:", "(unused)" };
		};
		lblP1.setText(names[0]);
		lblP2.setText(names[1]);
		lblP1Value.setText(String.format(Locale.ROOT, "%.2f", p1()));
		lblP2Value.setText(String.format(Locale.ROOT, "%.2f", p2()));
		scaleP2.setEnabled(!d.equals("exponential") && !d.equals("poisson"));
		layout();
	}

	private void changed() {
		if (btnLive.getSelection()) {
			plot();
		}
	}

	private void plot() {
		model.plot(comboDistribution.getText(), spinnerN.getSelection(), p1(), p2(), spinnerBins.getSelection());
	}

	public void setSummary(String text) {
		lblSummary.setText(text);
	}
}
