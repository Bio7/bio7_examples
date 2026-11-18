package gdal.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/*
 * The GUI of CoordinateConverter (editable with the WindowBuilder).
 */
public class CoordinateConverterGui extends Composite {

	private static final String[] SYSTEMS = { "EPSG:4326", "EPSG:32632", "EPSG:32633", "EPSG:25832", "EPSG:3857", "EPSG:31467", "EPSG:2056", "EPSG:27700",
			"+proj=longlat +datum=WGS84" };

	private Combo comboFrom;
	private Combo comboTo;
	private Label lblFrom;
	private Label lblTo;
	private Text textX;
	private Text textY;
	private Text textResult;
	private Text textList;
	private Text textListResult;

	public CoordinateConverterGui(Composite parent, CoordinateConverter model, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Group grpSystems = new Group(this, SWT.NONE);
		grpSystems.setText("Coordinate systems (EPSG code, WKT or PROJ string)");
		grpSystems.setLayout(new GridLayout(3, false));
		grpSystems.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblFromTitle = new Label(grpSystems, SWT.NONE);
		lblFromTitle.setText("From:");

		comboFrom = new Combo(grpSystems, SWT.NONE);
		comboFrom.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				lblFrom.setText(model.describe(comboFrom.getText()));
			}
		});
		comboFrom.setItems(SYSTEMS);
		GridData gdFrom = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gdFrom.widthHint = 200;
		comboFrom.setLayoutData(gdFrom);

		lblFrom = new Label(grpSystems, SWT.NONE);
		lblFrom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblToTitle = new Label(grpSystems, SWT.NONE);
		lblToTitle.setText("To:");

		comboTo = new Combo(grpSystems, SWT.NONE);
		comboTo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				lblTo.setText(model.describe(comboTo.getText()));
			}
		});
		comboTo.setItems(SYSTEMS);
		comboTo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		lblTo = new Label(grpSystems, SWT.NONE);
		lblTo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button btnSwap = new Button(grpSystems, SWT.NONE);
		btnSwap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String f = comboFrom.getText();
				comboFrom.setText(comboTo.getText());
				comboTo.setText(f);
			}
		});
		btnSwap.setText("Swap");

		Button btnUtm = new Button(grpSystems, SWT.NONE);
		btnUtm.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				run(() -> comboTo.setText(model.utmZone(comboFrom.getText(), textX.getText(), textY.getText())));
			}
		});
		btnUtm.setToolTipText("Sets the UTM zone (WGS84) of the point as target");
		btnUtm.setText("To: UTM zone of the point");
		new Label(grpSystems, SWT.NONE);

		Group grpPoint = new Group(this, SWT.NONE);
		grpPoint.setText("Point");
		grpPoint.setLayout(new GridLayout(5, false));
		grpPoint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblX = new Label(grpPoint, SWT.NONE);
		lblX.setText("x (lon, easting):");

		textX = new Text(grpPoint, SWT.BORDER);
		textX.setText("9.1720");
		textX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblY = new Label(grpPoint, SWT.NONE);
		lblY.setText("y (lat, northing):");

		textY = new Text(grpPoint, SWT.BORDER);
		textY.setText("47.6779");
		textY.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button btnConvert = new Button(grpPoint, SWT.NONE);
		btnConvert.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				run(() -> textResult.setText(model.convert(comboFrom.getText(), comboTo.getText(), textX.getText(), textY.getText())));
			}
		});
		btnConvert.setText("Convert");

		textResult = new Text(grpPoint, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI);
		GridData gdResult = new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1);
		gdResult.heightHint = 40;
		textResult.setLayoutData(gdResult);

		Group grpList = new Group(this, SWT.NONE);
		grpList.setText("List of points (one per line: x y or x;y)");
		grpList.setLayout(new GridLayout(3, false));
		grpList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		textList = new Text(grpList, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		textList.setText("9.0000 47.8400\n9.0120 47.8480\n9.1720 47.6779\n13.4050 52.5200");
		textList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Button btnConvertList = new Button(grpList, SWT.NONE);
		btnConvertList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				run(() -> textListResult.setText(model.convertList(comboFrom.getText(), comboTo.getText(), textList.getText())));
			}
		});
		btnConvertList.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		btnConvertList.setText("Convert ->");

		textListResult = new Text(grpList, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY);
		textListResult.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		comboFrom.setText("EPSG:4326");
		comboTo.setText("EPSG:32632");
	}

	/* An error (e.g., an unknown coordinate system, a wrong number) in the result field */
	private void run(Runnable r) {
		try {
			r.run();
		} catch (RuntimeException e) {
			textResult.setText("Error: " + e.getMessage());
		}
	}
}
