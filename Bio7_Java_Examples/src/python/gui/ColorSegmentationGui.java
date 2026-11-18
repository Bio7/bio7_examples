package python.gui;

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/*
 * The GUI of ColorSegmentation (editable with the WindowBuilder).
 */
public class ColorSegmentationGui extends Composite {

	private Spinner spinnerK;
	private Combo comboSpace;
	private Scale scaleBlur;
	private Label lblBlurValue;
	private Button btnLabels;
	private Button btnSegment;
	private Table table;
	private Label lblStatus;

	public ColorSegmentationGui(Composite parent, ColorSegmentation model, int style) {
		super(parent, style);
		setLayout(new GridLayout(3, false));

		Label lblK = new Label(this, SWT.NONE);
		lblK.setText("Clusters (k):");

		spinnerK = new Spinner(this, SWT.BORDER);
		spinnerK.setMaximum(16);
		spinnerK.setMinimum(2);
		spinnerK.setSelection(4);
		spinnerK.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label lblSpace = new Label(this, SWT.NONE);
		lblSpace.setText("Color space:");

		comboSpace = new Combo(this, SWT.READ_ONLY);
		comboSpace.setItems(new String[] { "Lab", "RGB", "HSV" });
		comboSpace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		comboSpace.select(0);

		Label lblBlur = new Label(this, SWT.NONE);
		lblBlur.setText("Smoothing:");

		scaleBlur = new Scale(this, SWT.NONE);
		scaleBlur.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lblBlurValue.setText(String.format(Locale.ROOT, "%.1f", blur()));
			}
		});
		scaleBlur.setMaximum(50);
		scaleBlur.setSelection(10);
		scaleBlur.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		lblBlurValue = new Label(this, SWT.NONE);
		lblBlurValue.setText("1.0");

		btnLabels = new Button(this, SWT.CHECK);
		btnLabels.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		btnLabels.setText("Show the label image too");

		btnSegment = new Button(this, SWT.NONE);
		btnSegment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.segment(spinnerK.getSelection(), comboSpace.getText(), blur(), btnLabels.getSelection());
			}
		});
		btnSegment.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		btnSegment.setText("Segment the current ImageJ image");

		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		TableColumn tblclmnCluster = new TableColumn(table, SWT.NONE);
		tblclmnCluster.setWidth(70);
		tblclmnCluster.setText("Cluster");

		TableColumn tblclmnColor = new TableColumn(table, SWT.NONE);
		tblclmnColor.setWidth(110);
		tblclmnColor.setText("Color (RGB)");

		TableColumn tblclmnShare = new TableColumn(table, SWT.RIGHT);
		tblclmnShare.setWidth(80);
		tblclmnShare.setText("Share %");

		lblStatus = new Label(this, SWT.NONE);
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		lblStatus.setText("Open an RGB image in ImageJ (else the synthetic cells are used).");
	}

	private double blur() {
		return scaleBlur.getSelection() / 10.0;
	}

	public void setClusters(List<ColorSegmentation.Cluster> clusters) {
		if (isDisposed()) {
			return;
		}
		table.removeAll();
		for (int i = 0; i < clusters.size(); i++) {
			ColorSegmentation.Cluster c = clusters.get(i);
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(new String[] { String.valueOf(i + 1), c.r() + ", " + c.g() + ", " + c.b(), String.format(Locale.ROOT, "%.1f", c.percent()) });
			/* The cell in the color of the cluster (new Color(r, g, b) without a device needs no dispose) */
			Color color = new Color(c.r(), c.g(), c.b());
			item.setBackground(1, color);
			item.setForeground(1, c.r() * 0.3 + c.g() * 0.59 + c.b() * 0.11 > 128 ? getDisplay().getSystemColor(SWT.COLOR_BLACK) : getDisplay().getSystemColor(SWT.COLOR_WHITE));
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
		btnSegment.setEnabled(!busy);
		if (busy) {
			lblStatus.setText("Running in Python ...");
		}
	}
}
