package gdal.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.resource.JFaceResources;

/*
 * The GUI of RasterTool (editable with the WindowBuilder).
 */
public class RasterToolGui extends Composite {

	private Text textSource;
	private Combo comboCrs;
	private Text textPixel;
	private Combo comboResampling;
	private Combo comboFormat;
	private Text textOutput;
	private Text textInfo;
	private Label lblStatus;
	private Button btnInfo;
	private Button btnConvert;
	private Button btnImageJ;
	private Button btnWorldWind;

	public RasterToolGui(Composite parent, RasterTool model, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Group grpSource = new Group(this, SWT.NONE);
		grpSource.setText("Raster file");
		grpSource.setLayout(new GridLayout(3, false));
		grpSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		textSource = new Text(grpSource, SWT.BORDER);
		textSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button btnBrowse = new Button(grpSource, SWT.NONE);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[] { "*.tif;*.tiff;*.vrt;*.img;*.asc;*.nc;*.jp2;*.png;*.jpg", "*.*" });
				String f = fd.open();
				if (f != null) {
					setSource(f);
				}
			}
		});
		btnBrowse.setText("Browse...");

		btnInfo = new Button(grpSource, SWT.NONE);
		btnInfo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.info(textSource.getText().trim());
			}
		});
		btnInfo.setText("Info");

		Group grpConversion = new Group(this, SWT.NONE);
		grpConversion.setText("Reproject and convert");
		grpConversion.setLayout(new GridLayout(4, false));
		grpConversion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblCrs = new Label(grpConversion, SWT.NONE);
		lblCrs.setText("Coordinate system:");

		comboCrs = new Combo(grpConversion, SWT.NONE);
		comboCrs.setToolTipText("Empty: the coordinate system of the file");
		comboCrs.setItems(new String[] { "", "EPSG:4326", "EPSG:3857", "EPSG:32632", "EPSG:25832", "EPSG:31467" });
		comboCrs.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboCrs.setText("EPSG:4326");

		Label lblPixel = new Label(grpConversion, SWT.NONE);
		lblPixel.setText("Pixel size:");

		textPixel = new Text(grpConversion, SWT.BORDER);
		textPixel.setMessage("empty: automatic");
		textPixel.setToolTipText("In the units of the coordinate system (degrees for EPSG:4326)");
		textPixel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblResampling = new Label(grpConversion, SWT.NONE);
		lblResampling.setText("Resampling:");

		comboResampling = new Combo(grpConversion, SWT.READ_ONLY);
		comboResampling.setItems(new String[] { "near", "bilinear", "cubic", "average", "mode", "max", "min" });
		comboResampling.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboResampling.select(1);

		Label lblFormat = new Label(grpConversion, SWT.NONE);
		lblFormat.setText("Format:");

		comboFormat = new Combo(grpConversion, SWT.READ_ONLY);
		comboFormat.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				suggestOutput();
			}
		});
		comboFormat.setItems(new String[] { "GTiff", "COG", "PNG", "JPEG", "AAIGrid", "netCDF" });
		comboFormat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboFormat.select(0);

		Label lblOutput = new Label(grpConversion, SWT.NONE);
		lblOutput.setText("Output file:");

		textOutput = new Text(grpConversion, SWT.BORDER);
		textOutput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		Composite compositeActions = new Composite(this, SWT.NONE);
		compositeActions.setLayout(new GridLayout(3, false));

		btnConvert = new Button(compositeActions, SWT.NONE);
		btnConvert.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.convert(textSource.getText().trim(), comboCrs.getText(), textPixel.getText(), comboResampling.getText(), comboFormat.getText(), textOutput.getText().trim());
			}
		});
		btnConvert.setText("Convert");

		btnImageJ = new Button(compositeActions, SWT.NONE);
		btnImageJ.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.openInImageJ(textOutput.getText().trim());
			}
		});
		btnImageJ.setText("Open output in ImageJ");

		btnWorldWind = new Button(compositeActions, SWT.NONE);
		btnWorldWind.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.showInWorldWind(textOutput.getText().trim());
			}
		});
		btnWorldWind.setText("Show output in WorldWind");

		textInfo = new Text(this, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		Font mono = JFaceResources.getTextFont();
		textInfo.setFont(mono);
		GridData gdInfo = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gdInfo.heightHint = 150;
		textInfo.setLayoutData(gdInfo);

		lblStatus = new Label(this, SWT.NONE);
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lblStatus.setText("Select a raster file (GeoTIFF, VRT, NetCDF, ... all formats of GDAL).");
	}

	public void setSource(String file) {
		textSource.setText(file);
		suggestOutput();
	}

	private void suggestOutput() {
		String src = textSource.getText().trim();
		if (src.isEmpty()) {
			return;
		}
		String ext = switch (comboFormat.getText()) {
		case "PNG" -> ".png";
		case "JPEG" -> ".jpg";
		case "AAIGrid" -> ".asc";
		case "netCDF" -> ".nc";
		default -> ".tif";
		};
		textOutput.setText(src.replaceAll("\\.[^./\\\\]+$", "") + "_converted" + ext);
	}

	public void setInfo(String info) {
		if (!isDisposed()) {
			textInfo.setText(info);
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
		for (Button b : new Button[] { btnInfo, btnConvert, btnImageJ, btnWorldWind }) {
			b.setEnabled(!busy);
		}
		if (busy) {
			lblStatus.setText("Running ...");
		}
	}
}
