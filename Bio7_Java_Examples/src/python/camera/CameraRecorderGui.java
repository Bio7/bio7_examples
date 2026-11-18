package python.camera;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/*
 * The GUI of CameraRecorder (editable with the WindowBuilder).
 */
public class CameraRecorderGui extends Composite {

	private Combo comboSource;
	private Spinner spinnerWidth;
	private Spinner spinnerHeight;
	private Button btnGray;
	private Spinner spinnerSeconds;
	private Spinner spinnerFrames;
	private Button btnPreview;
	private Spinner spinnerEvery;
	private Button btnStack;
	private Combo comboAnalysis;
	private Text textMacro;
	private Button btnStart;
	private Button btnStop;
	private Label lblStatus;

	public CameraRecorderGui(Composite parent, CameraRecorder model, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Group grpCamera = new Group(this, SWT.NONE);
		grpCamera.setText("Camera");
		grpCamera.setLayout(new GridLayout(6, false));
		grpCamera.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSource = new Label(grpCamera, SWT.NONE);
		lblSource.setText("Source:");

		comboSource = new Combo(grpCamera, SWT.NONE);
		comboSource.setToolTipText("0: the first camera, 1, 2, ...: other cameras, or the path of a video file");
		comboSource.setItems(new String[] { "0", "1", "2" });
		comboSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		comboSource.select(0);

		Button btnVideo = new Button(grpCamera, SWT.NONE);
		btnVideo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[] { "*.mp4;*.avi;*.mov;*.mkv;*.m4v", "*.*" });
				String f = fd.open();
				if (f != null) {
					comboSource.setText(f);
				}
			}
		});
		btnVideo.setText("Video file...");

		btnGray = new Button(grpCamera, SWT.CHECK);
		btnGray.setToolTipText("8-bit frames: a third of the memory, faster");
		btnGray.setText("Gray");

		Label lblSize = new Label(grpCamera, SWT.NONE);
		lblSize.setText("Size:");

		spinnerWidth = new Spinner(grpCamera, SWT.BORDER);
		spinnerWidth.setMaximum(4096);
		spinnerWidth.setMinimum(160);
		spinnerWidth.setIncrement(160);
		spinnerWidth.setSelection(640);

		Label lblX = new Label(grpCamera, SWT.NONE);
		lblX.setText("x");

		spinnerHeight = new Spinner(grpCamera, SWT.BORDER);
		spinnerHeight.setMaximum(2160);
		spinnerHeight.setMinimum(120);
		spinnerHeight.setIncrement(120);
		spinnerHeight.setSelection(480);

		Label lblSizeHint = new Label(grpCamera, SWT.NONE);
		lblSizeHint.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblSizeHint.setText("(requested, the camera chooses the nearest)");

		Group grpRecording = new Group(this, SWT.NONE);
		grpRecording.setText("Recording");
		grpRecording.setLayout(new GridLayout(6, false));
		grpRecording.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSeconds = new Label(grpRecording, SWT.NONE);
		lblSeconds.setText("Seconds:");

		spinnerSeconds = new Spinner(grpRecording, SWT.BORDER);
		spinnerSeconds.setMaximum(36000);
		spinnerSeconds.setMinimum(1);
		spinnerSeconds.setSelection(20);

		Label lblFrames = new Label(grpRecording, SWT.NONE);
		lblFrames.setText("Max. frames:");

		spinnerFrames = new Spinner(grpRecording, SWT.BORDER);
		spinnerFrames.setMaximum(1000000);
		spinnerFrames.setMinimum(1);
		spinnerFrames.setIncrement(100);
		spinnerFrames.setSelection(1000);

		btnStack = new Button(grpRecording, SWT.CHECK);
		btnStack.setToolTipText("Off: only the measurements (no memory for the frames, any length)");
		btnStack.setSelection(true);
		btnStack.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnStack.setText("Keep the frames as stack");

		btnPreview = new Button(grpRecording, SWT.CHECK);
		btnPreview.setSelection(true);
		btnPreview.setText("Live view, every");

		spinnerEvery = new Spinner(grpRecording, SWT.BORDER);
		spinnerEvery.setMaximum(100);
		spinnerEvery.setMinimum(1);
		spinnerEvery.setSelection(2);

		Label lblEvery = new Label(grpRecording, SWT.NONE);
		lblEvery.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		lblEvery.setText("frames (off: faster)");

		Group grpAnalysis = new Group(this, SWT.NONE);
		grpAnalysis.setText("Analysis of each frame");
		grpAnalysis.setLayout(new GridLayout(2, false));
		grpAnalysis.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Label lblAnalysis = new Label(grpAnalysis, SWT.NONE);
		lblAnalysis.setText("Macro:");

		comboAnalysis = new Combo(grpAnalysis, SWT.READ_ONLY);
		comboAnalysis.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				textMacro.setEnabled(comboAnalysis.getSelectionIndex() == 2);
			}
		});
		comboAnalysis.setItems(new String[] { "None (only recording)", "Java macro (javaMacro in CameraRecorder.java: mean, motion, bright areas)",
				"ImageJ macro (below)" });
		comboAnalysis.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboAnalysis.select(1);

		textMacro = new Text(grpAnalysis, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		textMacro.setEnabled(false);
		textMacro.setFont(JFaceResources.getTextFont());
		textMacro.setText("// Runs on each frame (the current image, not shown).\n// Return the measurements as name=value pairs:\n"
				+ "getStatistics(area, mean, min, max, std);\nreturn \"Mean=\" + mean + \",StdDev=\" + std + \",Max=\" + max;\n");
		GridData gdMacro = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdMacro.heightHint = 90;
		textMacro.setLayoutData(gdMacro);

		Composite compositeButtons = new Composite(this, SWT.NONE);
		compositeButtons.setLayout(new GridLayout(2, false));

		btnStart = new Button(compositeButtons, SWT.NONE);
		btnStart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.start(new CameraRecorder.Settings(comboSource.getText(), spinnerWidth.getSelection(), spinnerHeight.getSelection(), btnGray.getSelection(),
						spinnerSeconds.getSelection(), spinnerFrames.getSelection(), btnPreview.getSelection(), spinnerEvery.getSelection(),
						comboAnalysis.getSelectionIndex(), textMacro.getText(), btnStack.getSelection()));
			}
		});
		btnStart.setText("Start recording");

		btnStop = new Button(compositeButtons, SWT.NONE);
		btnStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.stop();
			}
		});
		btnStop.setEnabled(false);
		btnStop.setText("Stop");

		lblStatus = new Label(this, SWT.NONE);
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lblStatus.setText("Start the recording (macOS: allow Bio7 to use the camera).");
	}

	public void setRecording(boolean recording) {
		if (isDisposed()) {
			return;
		}
		btnStart.setEnabled(!recording);
		btnStop.setEnabled(recording);
		if (recording) {
			lblStatus.setText("Opening the camera ...");
		}
	}

	public void setStatus(String text) {
		if (!isDisposed()) {
			lblStatus.setText(text);
		}
	}
}
