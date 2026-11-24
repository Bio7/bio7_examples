package geo.gdal.gui;

import java.util.List;
import java.util.Locale;

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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/*
 * The GUI of GeoreferenceImage (editable with the WindowBuilder).
 */
public class GeoreferenceImageGui extends Composite {

	private Text textX;
	private Text textY;
	private Table table;
	private Combo comboCrs;
	private Combo comboMethod;
	private Label lblStatus;
	private Button btnTest;
	private Button btnAdd;
	private Button btnRemove;
	private Button btnClear;
	private Button btnGeoreference;
	private Button btnWorldWind;

	public GeoreferenceImageGui(Composite parent, GeoreferenceImage model, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		btnTest = new Button(this, SWT.NONE);
		btnTest.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.createTestScan();
			}
		});
		btnTest.setText("Create test scan (with control points)");

		Group grpPoint = new Group(this, SWT.NONE);
		grpPoint.setText("Control point: click a point in the image (point tool), enter its map coordinates");
		grpPoint.setLayout(new GridLayout(5, false));
		grpPoint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblX = new Label(grpPoint, SWT.NONE);
		lblX.setText("Map X:");

		textX = new Text(grpPoint, SWT.BORDER);
		textX.setMessage("e.g. 500300 or 9.004");
		textX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblY = new Label(grpPoint, SWT.NONE);
		lblY.setText("Map Y:");

		textY = new Text(grpPoint, SWT.BORDER);
		textY.setMessage("e.g. 5299750 or 47.85");
		textY.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		btnAdd = new Button(grpPoint, SWT.NONE);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.addPoint(textX.getText(), textY.getText());
			}
		});
		btnAdd.setText("Add point");

		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
		GridData gdTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gdTable.heightHint = 120;
		table.setLayoutData(gdTable);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tblclmnNr = new TableColumn(table, SWT.RIGHT);
		tblclmnNr.setWidth(40);
		tblclmnNr.setText("#");

		TableColumn tblclmnPixel = new TableColumn(table, SWT.RIGHT);
		tblclmnPixel.setWidth(80);
		tblclmnPixel.setText("Pixel");

		TableColumn tblclmnLine = new TableColumn(table, SWT.RIGHT);
		tblclmnLine.setWidth(80);
		tblclmnLine.setText("Line");

		TableColumn tblclmnMapX = new TableColumn(table, SWT.RIGHT);
		tblclmnMapX.setWidth(110);
		tblclmnMapX.setText("Map X");

		TableColumn tblclmnMapY = new TableColumn(table, SWT.RIGHT);
		tblclmnMapY.setWidth(110);
		tblclmnMapY.setText("Map Y");

		TableColumn tblclmnResidual = new TableColumn(table, SWT.RIGHT);
		tblclmnResidual.setWidth(90);
		tblclmnResidual.setText("Residual");

		Composite compositePoints = new Composite(this, SWT.NONE);
		compositePoints.setLayout(new GridLayout(2, false));

		btnRemove = new Button(compositePoints, SWT.NONE);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.removePoint(table.getSelectionIndex());
			}
		});
		btnRemove.setText("Remove selected point");

		btnClear = new Button(compositePoints, SWT.NONE);
		btnClear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.clearPoints();
			}
		});
		btnClear.setText("Clear");

		Group grpTransformation = new Group(this, SWT.NONE);
		grpTransformation.setText("Transformation");
		grpTransformation.setLayout(new GridLayout(4, false));
		grpTransformation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblCrs = new Label(grpTransformation, SWT.NONE);
		lblCrs.setText("Coordinate system:");

		comboCrs = new Combo(grpTransformation, SWT.NONE);
		comboCrs.setItems(new String[] { "EPSG:32632", "EPSG:4326", "EPSG:25832", "EPSG:3857", "EPSG:31467", "EPSG:2056" });
		comboCrs.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboCrs.select(0);

		Label lblMethod = new Label(grpTransformation, SWT.NONE);
		lblMethod.setText("Method:");

		comboMethod = new Combo(grpTransformation, SWT.READ_ONLY);
		comboMethod.setItems(new String[] { "Polynomial 1 (affine, 3+ points)", "Polynomial 2 (6+ points)", "Thin plate spline (rubber sheet)" });
		comboMethod.select(0);

		Composite compositeActions = new Composite(this, SWT.NONE);
		compositeActions.setLayout(new GridLayout(2, false));

		btnGeoreference = new Button(compositeActions, SWT.NONE);
		btnGeoreference.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.georeference(comboCrs.getText().trim(), comboMethod.getSelectionIndex());
			}
		});
		btnGeoreference.setText("Georeference (GeoTIFF, open in ImageJ)");

		btnWorldWind = new Button(compositeActions, SWT.NONE);
		btnWorldWind.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.showInWorldWind();
			}
		});
		btnWorldWind.setText("Show in WorldWind");

		lblStatus = new Label(this, SWT.WRAP);
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lblStatus.setText("Create the test scan, or open an image in ImageJ and add at least 3 control points.");
	}

	public void setPoints(List<GeoreferenceImage.Gcp> points, double[] residuals) {
		if (isDisposed()) {
			return;
		}
		table.removeAll();
		for (int i = 0; i < points.size(); i++) {
			GeoreferenceImage.Gcp g = points.get(i);
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(new String[] { String.valueOf(i + 1), String.format(Locale.ROOT, "%.1f", g.pixel()), String.format(Locale.ROOT, "%.1f", g.line()),
					String.format(Locale.ROOT, "%.4f", g.x()), String.format(Locale.ROOT, "%.4f", g.y()),
					residuals == null ? "" : String.format(Locale.ROOT, "%.3f", residuals[i]) });
		}
	}

	public void setCrs(String crs) {
		if (!isDisposed()) {
			comboCrs.setText(crs);
		}
	}

	public void setStatus(String text) {
		if (!isDisposed()) {
			lblStatus.setText(text);
			layout(new org.eclipse.swt.widgets.Control[] { lblStatus });
		}
	}

	public void setBusy(boolean busy) {
		if (isDisposed()) {
			return;
		}
		for (Button b : new Button[] { btnTest, btnAdd, btnRemove, btnClear, btnGeoreference, btnWorldWind }) {
			b.setEnabled(!busy);
		}
		if (busy) {
			setStatus("Running ...");
		}
	}
}
