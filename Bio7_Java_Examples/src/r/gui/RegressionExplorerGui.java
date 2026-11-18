package r.gui;

import java.util.Locale;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/*
 * The GUI of RegressionExplorer (editable with the WindowBuilder).
 */
public class RegressionExplorerGui extends Composite {

	private Combo comboData;
	private Combo comboX;
	private Combo comboY;
	private Combo comboModel;
	private Spinner spinnerDegree;
	private Scale scaleSpan;
	private Label lblSpanValue;
	private Text textSummary;
	private Label lblStatus;
	private Button btnFit;

	public RegressionExplorerGui(Composite parent, RegressionExplorer model, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Group grpData = new Group(this, SWT.NONE);
		grpData.setText("Data and model");
		grpData.setLayout(new GridLayout(6, false));
		grpData.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblData = new Label(grpData, SWT.NONE);
		lblData.setText("Data set:");

		comboData = new Combo(grpData, SWT.NONE);
		comboData.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.dataSet(comboData.getText());
			}
		});
		comboData.setToolTipText("A data frame of R (also your own: type its name and press Enter)");
		comboData.setItems(new String[] { "cars", "trees", "mtcars", "iris", "airquality", "faithful" });
		comboData.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboData.select(0);
		comboData.addListener(SWT.DefaultSelection, e -> model.dataSet(comboData.getText().trim()));

		Label lblX = new Label(grpData, SWT.NONE);
		lblX.setText("x:");

		comboX = new Combo(grpData, SWT.READ_ONLY);
		comboX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblY = new Label(grpData, SWT.NONE);
		lblY.setText("y:");

		comboY = new Combo(grpData, SWT.READ_ONLY);
		comboY.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblModel = new Label(grpData, SWT.NONE);
		lblModel.setText("Model:");

		comboModel = new Combo(grpData, SWT.READ_ONLY);
		comboModel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				spinnerDegree.setEnabled(comboModel.getSelectionIndex() == 1);
				scaleSpan.setEnabled(comboModel.getSelectionIndex() == 3);
			}
		});
		comboModel.setItems(new String[] { "Linear", "Polynomial", "Logarithmic", "Loess (smooth)" });
		comboModel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboModel.select(0);

		Label lblDegree = new Label(grpData, SWT.NONE);
		lblDegree.setText("Degree:");

		spinnerDegree = new Spinner(grpData, SWT.BORDER);
		spinnerDegree.setEnabled(false);
		spinnerDegree.setMaximum(6);
		spinnerDegree.setMinimum(2);
		spinnerDegree.setSelection(2);

		Label lblSpan = new Label(grpData, SWT.NONE);
		lblSpan.setText("Span:");

		scaleSpan = new Scale(grpData, SWT.NONE);
		scaleSpan.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lblSpanValue.setText(String.format(Locale.ROOT, "%.2f", span()));
			}
		});
		scaleSpan.setEnabled(false);
		scaleSpan.setMaximum(100);
		scaleSpan.setMinimum(20);
		scaleSpan.setSelection(75);
		scaleSpan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		lblSpanValue = new Label(grpData, SWT.NONE);
		lblSpanValue.setText("0.75");

		btnFit = new Button(this, SWT.NONE);
		btnFit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.fit(comboData.getText().trim(), comboX.getText(), comboY.getText(), comboModel.getSelectionIndex(), spinnerDegree.getSelection(), span());
			}
		});
		btnFit.setText("Fit and plot in R");

		textSummary = new Text(this, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		textSummary.setFont(JFaceResources.getTextFont());
		textSummary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		lblStatus = new Label(this, SWT.NONE);
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	}

	private double span() {
		return scaleSpan.getSelection() / 100.0;
	}

	public void setVariables(String[] vars) {
		if (isDisposed()) {
			return;
		}
		comboX.setItems(vars);
		comboY.setItems(vars);
		if (vars.length > 1) {
			comboX.select(0);
			comboY.select(1);
		}
	}

	public void setSummary(String text) {
		if (!isDisposed()) {
			textSummary.setText(text);
		}
	}

	public void setStatus(String text) {
		if (!isDisposed()) {
			lblStatus.setText(text);
		}
	}

	public void setBusy(boolean busy) {
		if (isDisposed()) {
			return;
		}
		btnFit.setEnabled(!busy);
		if (busy) {
			lblStatus.setText("Running in R ...");
		}
	}
}
