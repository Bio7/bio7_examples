package python.templatematching;

import java.util.List;

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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/*
 * The GUI of TemplateMatching (editable with the WindowBuilder). The buttons
 * call the methods of the model, the model calls setTemplates, setResults,
 * setStatus and setBusy.
 */
public class TemplateMatchingGui extends Composite {

	private Text textName;
	private org.eclipse.swt.widgets.List listTemplates;
	private Combo comboMethod;
	private Scale scaleThreshold;
	private Label lblThresholdValue;
	private Button btnMultiscale;
	private Table table;
	private Label lblStatus;
	private Button btnTestImage;
	private Button btnAdd;
	private Button btnClear;
	private Button btnDetect;

	public TemplateMatchingGui(Composite parent, TemplateMatching model, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));

		Group grpImage = new Group(this, SWT.NONE);
		grpImage.setText("Image and templates");
		grpImage.setLayout(new GridLayout(3, false));
		grpImage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		btnTestImage = new Button(grpImage, SWT.NONE);
		btnTestImage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.createTestImage();
			}
		});
		btnTestImage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		btnTestImage.setText("Create test image (with 4 templates)");

		Label lblName = new Label(grpImage, SWT.NONE);
		lblName.setText("Name:");

		textName = new Text(grpImage, SWT.BORDER);
		textName.setMessage("e.g. cell");
		textName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		btnAdd = new Button(grpImage, SWT.NONE);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.addTemplate(textName.getText());
			}
		});
		btnAdd.setToolTipText("The selection (rectangle) of the current ImageJ image as template");
		btnAdd.setText("Add selection");

		listTemplates = new org.eclipse.swt.widgets.List(grpImage, SWT.BORDER | SWT.V_SCROLL);
		GridData gdList = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdList.heightHint = 70;
		listTemplates.setLayoutData(gdList);

		btnClear = new Button(grpImage, SWT.NONE);
		btnClear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.clearTemplates();
			}
		});
		btnClear.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		btnClear.setText("Clear");

		Group grpDetection = new Group(this, SWT.NONE);
		grpDetection.setText("Detection");
		grpDetection.setLayout(new GridLayout(3, false));
		grpDetection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		Label lblMethod = new Label(grpDetection, SWT.NONE);
		lblMethod.setText("Method:");

		comboMethod = new Combo(grpDetection, SWT.READ_ONLY);
		comboMethod.setItems(new String[] { "TM_CCOEFF_NORMED", "TM_CCORR_NORMED", "TM_SQDIFF_NORMED" });
		comboMethod.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		comboMethod.select(0);

		Label lblThreshold = new Label(grpDetection, SWT.NONE);
		lblThreshold.setText("Threshold:");

		scaleThreshold = new Scale(grpDetection, SWT.NONE);
		scaleThreshold.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lblThresholdValue.setText(String.format("%.2f", threshold()));
			}
		});
		scaleThreshold.setMinimum(30);
		scaleThreshold.setMaximum(99);
		scaleThreshold.setSelection(70);
		scaleThreshold.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		lblThresholdValue = new Label(grpDetection, SWT.NONE);
		lblThresholdValue.setText("0.70");

		btnMultiscale = new Button(grpDetection, SWT.CHECK);
		btnMultiscale.setSelection(true);
		btnMultiscale.setToolTipText("The templates also 10 % smaller and larger");
		btnMultiscale.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		btnMultiscale.setText("Multi-scale (0.9, 1.0, 1.1)");

		btnDetect = new Button(grpDetection, SWT.NONE);
		btnDetect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.detect(comboMethod.getText(), threshold(), btnMultiscale.getSelection());
			}
		});
		btnDetect.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		btnDetect.setText("Detect and select in ImageJ");

		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tblclmnTemplate = new TableColumn(table, SWT.NONE);
		tblclmnTemplate.setWidth(110);
		tblclmnTemplate.setText("Template");

		TableColumn tblclmnX = new TableColumn(table, SWT.RIGHT);
		tblclmnX.setWidth(60);
		tblclmnX.setText("X");

		TableColumn tblclmnY = new TableColumn(table, SWT.RIGHT);
		tblclmnY.setWidth(60);
		tblclmnY.setText("Y");

		TableColumn tblclmnWidth = new TableColumn(table, SWT.RIGHT);
		tblclmnWidth.setWidth(60);
		tblclmnWidth.setText("Width");

		TableColumn tblclmnHeight = new TableColumn(table, SWT.RIGHT);
		tblclmnHeight.setWidth(60);
		tblclmnHeight.setText("Height");

		TableColumn tblclmnScore = new TableColumn(table, SWT.RIGHT);
		tblclmnScore.setWidth(70);
		tblclmnScore.setText("Score");

		lblStatus = new Label(this, SWT.NONE);
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		lblStatus.setText("Create the test image, or select a shape in an ImageJ image and add it as template.");
	}

	private double threshold() {
		return scaleThreshold.getSelection() / 100.0;
	}

	public void setTemplates(List<String> names) {
		if (isDisposed()) {
			return;
		}
		listTemplates.setItems(names.toArray(new String[0]));
	}

	public void setResults(List<String> names, double[][] rows) {
		if (isDisposed()) {
			return;
		}
		table.removeAll();
		for (double[] r : rows) {
			TableItem item = new TableItem(table, SWT.NONE);
			int t = (int) r[0];
			item.setText(new String[] { t < names.size() ? names.get(t) : "?", String.valueOf((int) r[1]), String.valueOf((int) r[2]), String.valueOf((int) r[3]),
					String.valueOf((int) r[4]), String.format("%.3f", r[5]) });
		}
	}

	public void setStatus(String text) {
		if (!isDisposed()) {
			lblStatus.setText(text);
			lblStatus.getParent().layout(new org.eclipse.swt.widgets.Control[] { lblStatus });
		}
	}

	public void setBusy(boolean busy) {
		if (isDisposed()) {
			return;
		}
		for (Button b : new Button[] { btnTestImage, btnAdd, btnClear, btnDetect }) {
			b.setEnabled(!busy);
		}
		if (busy) {
			setStatus("Running in Python ...");
		}
	}
}
