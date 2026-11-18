package deeplearning;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.collection.CustomView;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Segments images with a deep learning model trained in Python (e.g., PyTorch
 * or Keras exported to ONNX) with the Deep Java Library (DJL, https://djl.ai).
 * <p>
 * The example checks if DJL and an engine are installed. If not, it shows the
 * Maven dependencies and opens the library installer (Preferences > Bio7 >
 * Preferences Java Dynamic Compilation > Preferences Libraries > Maven...).
 * DJL is used with reflection, so the example compiles also without the
 * library.
 * <p>
 * A model is loaded in the custom view (an ONNX file with the ONNX Runtime
 * engine; PyTorch or TensorFlow if their engine is installed). The active
 * ImageJ image (also a stack) or all images of a folder and its subfolders
 * (batch mode) are segmented: a mask (0/255) for two classes, else a label
 * image with the class of each pixel.
 * <p>
 * The model of the example: train_blob_segmentation.py (in this folder) trains
 * a small U-Net which segments bright blobs (e.g., File > Open Samples > Blobs
 * in ImageJ) and exports blob_segmentation.onnx: input [N, 1, H, W] scaled to
 * 0 - 1 (Min-Max), output [N, 2, H, W] (the logits of background and blob), H
 * and W a multiple of 4 (Pad to Multiple of 16).
 */
public class DeepLearningSegmentation extends com.eco.bio7.compile.Model {

	/* The Maven dependencies (paste them in the Maven dialog of the library installer) */
	static final String DJL_VERSION = "0.40.0";
	static final String MAVEN_DEPENDENCIES = """
			<properties>
			  <djl.version>%s</djl.version>
			</properties>
			<dependencies>
			  <dependency>
			    <groupId>ai.djl</groupId>
			    <artifactId>api</artifactId>
			    <version>${djl.version}</version>
			  </dependency>
			  <dependency>
			    <groupId>ai.djl.onnxruntime</groupId>
			    <artifactId>onnxruntime-engine</artifactId>
			    <version>${djl.version}</version>
			  </dependency>
			</dependencies>
			""".formatted(DJL_VERSION);
	/* The library installer (the Maven dialog is opened with the button "Maven...") */
	static final String LIBRARY_PREFERENCE_PAGE = "com.eco.bio7.preferences_compiler";
	static final String[] ENGINES = { "OnnxRuntime", "PyTorch", "TensorFlow" };
	static final String[] NORMALIZATIONS = { "Min-Max (0 - 1)", "Divide by 255", "ImageNet (mean, std)", "None" };
	static final String[] IMAGE_EXTENSIONS = { ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp", ".gif" };

	private Display display;
	private Label libraryStatus;
	private Text dependenciesText;
	private Text modelPath;
	private Combo engineCombo, normalizationCombo;
	private Spinner channelsSpinner, padSpinner, widthSpinner, heightSpinner;
	private Label modelInfo;
	private Button loadButton, segmentButton, batchButton, cancelButton, subfoldersCheck;
	private Text inputFolder, outputFolder, log;
	private ProgressBar progress;

	private Djl djl;
	private Segmenter segmenter;
	private volatile boolean cancel;
	private volatile boolean busy;

	@Override
	public void setup() {
		/* The custom view in the UI thread (also if setup is called in another thread) */
		Display.getDefault().syncExec(() -> {
			CustomView view = new CustomView();
			Composite parent = view.getComposite("Deep Learning Segmentation");
			createControls(parent);
		});
	}

	/* ------------------------------ The custom view ------------------------------ */

	private void createControls(Composite parent) {
		display = parent.getDisplay();
		parent.setLayout(new GridLayout(1, false));
		/* The library */
		Group libraryGroup = group(parent, "Deep Java Library (DJL)", 3);
		libraryStatus = new Label(libraryGroup, SWT.WRAP);
		libraryStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		Button checkButton = button(libraryGroup, "Check Again", "Checks again if the library is installed");
		checkButton.addListener(SWT.Selection, _ -> checkLibrary());
		Button installerButton = button(libraryGroup, "Open Library Installer...",
				"Opens the library installer: Maven... (paste the dependencies, enable \"Download dependencies\")");
		installerButton.addListener(SWT.Selection, _ -> openInstaller());
		Button copyButton = button(libraryGroup, "Copy Maven Dependencies", "Copies the dependencies to the clipboard");
		copyButton.addListener(SWT.Selection, _ -> copyDependencies());
		dependenciesText = new Text(libraryGroup, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL);
		GridData depData = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		depData.heightHint = 150;
		dependenciesText.setLayoutData(depData);
		dependenciesText.setText("Install: Open Library Installer... > Maven... > paste the dependencies > enable \"Download dependencies\""
				+ " > Download (the libraries are added to the compiler libraries). Then compile and run this example again.\n\n"
				+ MAVEN_DEPENDENCIES);
		/* The model */
		Group modelGroup = group(parent, "Model", 4);
		label(modelGroup, "Model File:");
		modelPath = new Text(modelGroup, SWT.BORDER);
		modelPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		modelPath.setMessage("An ONNX file (.onnx), a PyTorch TorchScript file (.pt) or a TensorFlow SavedModel folder");
		Button browseModel = button(modelGroup, "Browse...", "Selects the model file");
		browseModel.addListener(SWT.Selection, _ -> {
			FileDialog dialog = new FileDialog(parent.getShell(), SWT.OPEN);
			dialog.setFilterExtensions(new String[] { "*.onnx;*.pt;*.pth;*.pb", "*.*" });
			String file = dialog.open();
			if (file != null) {
				modelPath.setText(file);
			}
		});
		label(modelGroup, "Engine:");
		engineCombo = new Combo(modelGroup, SWT.READ_ONLY);
		engineCombo.setItems(ENGINES);
		engineCombo.select(0);
		engineCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		label(modelGroup, "Normalization:");
		normalizationCombo = new Combo(modelGroup, SWT.READ_ONLY);
		normalizationCombo.setItems(NORMALIZATIONS);
		normalizationCombo.select(0);
		normalizationCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		label(modelGroup, "Input Channels:");
		channelsSpinner = spinner(modelGroup, 1, 1, 4, "1: gray (RGB images are converted), 3: RGB (gray images are repeated)");
		label(modelGroup, "Pad to Multiple of:");
		padSpinner = spinner(modelGroup, 16, 1, 256, "The image is padded (mirrored edges) to a multiple of this size (e.g., a U-Net)");
		label(modelGroup, "Fixed Width:");
		widthSpinner = spinner(modelGroup, 0, 0, 8192, "0: the size of the image; else the image is resized to the input size of the model");
		label(modelGroup, "Fixed Height:");
		heightSpinner = spinner(modelGroup, 0, 0, 8192, "0: the size of the image; else the image is resized to the input size of the model");
		loadButton = button(modelGroup, "Load Model", "Loads the model with the selected engine");
		loadButton.addListener(SWT.Selection, _ -> loadModel());
		modelInfo = new Label(modelGroup, SWT.WRAP);
		modelInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		modelInfo.setText("No model loaded.");
		/* The segmentation */
		Group segmentGroup = group(parent, "Segmentation", 4);
		segmentButton = button(segmentGroup, "Segment Active Image", "Segments the active ImageJ image (all slices of a stack)");
		((GridData) segmentButton.getLayoutData()).horizontalSpan = 4;
		segmentButton.addListener(SWT.Selection, _ -> segmentActiveImage());
		label(segmentGroup, "Input Folder:");
		inputFolder = new Text(segmentGroup, SWT.BORDER);
		inputFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		folderButton(segmentGroup, inputFolder, "The folder of the images");
		label(segmentGroup, "Output Folder:");
		outputFolder = new Text(segmentGroup, SWT.BORDER);
		outputFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		outputFolder.setMessage("Empty: the folder \"segmentation\" in the input folder");
		folderButton(segmentGroup, outputFolder, "The folder of the results (the subfolders are created like in the input folder)");
		subfoldersCheck = new Button(segmentGroup, SWT.CHECK);
		subfoldersCheck.setText("Include Subfolders");
		subfoldersCheck.setSelection(true);
		subfoldersCheck.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		batchButton = button(segmentGroup, "Run Batch", "Segments all images of the folder (and the subfolders)");
		batchButton.addListener(SWT.Selection, _ -> runBatch());
		cancelButton = button(segmentGroup, "Cancel", "Stops the batch after the current image");
		cancelButton.addListener(SWT.Selection, _ -> cancel = true);
		progress = new ProgressBar(segmentGroup, SWT.HORIZONTAL);
		progress.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		log = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		log.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.addListener(SWT.Dispose, _ -> closeModel());
		checkLibrary();
		updateButtons();
		parent.layout(true, true);
	}

	/* ------------------------------ The library ------------------------------ */

	/* Checks if DJL and an engine can be loaded (with the class loader of the compiled example) */
	private void checkLibrary() {
		djl = Djl.find(getClass().getClassLoader());
		if (djl == null) {
			libraryStatus.setText("✗ The Deep Java Library is not installed. Open the library installer, click \"Maven...\", paste the "
					+ "dependencies below, enable \"Download dependencies\" and download. Then compile and run this example again.");
			libraryStatus.setForeground(display.getSystemColor(SWT.COLOR_RED));
		} else {
			Set<String> engines = djl.engines();
			if (engines.isEmpty()) {
				libraryStatus.setText("✗ DJL " + djl.version() + " is installed, but no engine (e.g., ai.djl.onnxruntime:onnxruntime-engine).");
				libraryStatus.setForeground(display.getSystemColor(SWT.COLOR_RED));
			} else {
				libraryStatus.setText("✓ DJL " + djl.version() + " is installed. Engines: " + String.join(", ", engines));
				libraryStatus.setForeground(display.getSystemColor(SWT.COLOR_DARK_GREEN));
				for (int i = 0; i < ENGINES.length; i++) {
					if (engines.contains(ENGINES[i])) {
						engineCombo.select(i);
						break;
					}
				}
			}
		}
		libraryStatus.getParent().layout(true, true);
		updateButtons();
	}

	private void openInstaller() {
		PreferencesUtil.createPreferenceDialogOn(libraryStatus.getShell(), LIBRARY_PREFERENCE_PAGE, null, null).open();
		checkLibrary();
	}

	private void copyDependencies() {
		Clipboard clipboard = new Clipboard(display);
		clipboard.setContents(new Object[] { MAVEN_DEPENDENCIES }, new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
		log("The Maven dependencies have been copied to the clipboard.");
	}

	/* ------------------------------ The model ------------------------------ */

	private void loadModel() {
		if (djl == null || busy) {
			return;
		}
		String path = modelPath.getText().trim();
		if (path.isEmpty() || !new File(path).exists()) {
			log("The model file doesn't exist: " + path);
			return;
		}
		String engine = engineCombo.getText();
		closeModel();
		busy = true;
		updateButtons();
		modelInfo.setText("Loading the model...");
		background(() -> {
			try {
				Segmenter s = new Segmenter(djl, Paths.get(path), engine);
				segmenter = s;
				long[] input = s.inputShape();
				display.asyncExec(() -> {
					/* The channels and the fixed size of the model (if the model declares them) */
					if (input != null && input.length == 4) {
						if (input[1] > 0) {
							channelsSpinner.setSelection((int) input[1]);
						}
						widthSpinner.setSelection(input[3] > 0 ? (int) input[3] : 0);
						heightSpinner.setSelection(input[2] > 0 ? (int) input[2] : 0);
					}
					modelInfo.setText(new File(path).getName() + " (" + engine + "), input " + Segmenter.shapeText(input));
				});
				log("Model loaded: " + path + ", engine " + engine + ", input " + Segmenter.shapeText(input));
			} catch (Throwable e) {
				log("The model could not be loaded: " + Djl.message(e));
				display.asyncExec(() -> modelInfo.setText("No model loaded."));
			}
		});
	}

	private void closeModel() {
		Segmenter s = segmenter;
		segmenter = null;
		if (s != null) {
			s.close();
		}
	}

	/* The settings of the view (read in the UI thread) */
	private Segmenter.Settings settings() {
		Segmenter.Settings s = new Segmenter.Settings();
		s.channels = channelsSpinner.getSelection();
		s.padMultiple = padSpinner.getSelection();
		s.width = widthSpinner.getSelection();
		s.height = heightSpinner.getSelection();
		s.normalization = normalizationCombo.getSelectionIndex();
		return s;
	}

	/* ------------------------------ The segmentation ------------------------------ */

	private void segmentActiveImage() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			log("No image is opened (e.g., File > Open Samples > Blobs in ImageJ).");
			return;
		}
		Segmenter s = segmenter;
		if (s == null || busy) {
			return;
		}
		Segmenter.Settings settings = settings();
		busy = true;
		updateButtons();
		background(() -> {
			try {
				long start = System.nanoTime();
				ImagePlus result = s.segment(imp, settings);
				log(String.format(Locale.ROOT, "%s segmented in %.2f s: %s", imp.getTitle(), (System.nanoTime() - start) / 1e9,
						result.getTitle()));
				display.asyncExec(result::show);
			} catch (Throwable e) {
				log("The segmentation failed: " + Djl.message(e));
			}
		});
	}

	private void runBatch() {
		Segmenter s = segmenter;
		if (s == null || busy) {
			return;
		}
		Path input = Paths.get(inputFolder.getText().trim());
		if (inputFolder.getText().isBlank() || !Files.isDirectory(input)) {
			log("The input folder doesn't exist: " + inputFolder.getText());
			return;
		}
		Path output = outputFolder.getText().isBlank() ? input.resolve("segmentation") : Paths.get(outputFolder.getText().trim());
		boolean subfolders = subfoldersCheck.getSelection();
		Segmenter.Settings settings = settings();
		busy = true;
		cancel = false;
		updateButtons();
		background(() -> {
			List<Path> images;
			try (Stream<Path> walk = Files.walk(input, subfolders ? Integer.MAX_VALUE : 1)) {
				/* Not the results of a former batch in the input folder */
				images = walk.filter(Files::isRegularFile).filter(p -> !p.startsWith(output)).filter(DeepLearningSegmentation::isImage)
						.sorted().collect(Collectors.toList());
			} catch (Exception e) {
				log("The folder could not be read: " + Djl.message(e));
				return;
			}
			log("Batch: " + images.size() + " images in " + input + (subfolders ? " (with subfolders)" : "") + " -> " + output);
			int done = 0, failed = 0;
			long start = System.nanoTime();
			for (int i = 0; i < images.size() && !cancel; i++) {
				Path file = images.get(i);
				Path relative = input.relativize(file);
				final int index = i;
				display.asyncExec(() -> {
					if (!progress.isDisposed()) {
						progress.setMaximum(images.size());
						progress.setSelection(index);
					}
				});
				try {
					ImagePlus imp = IJ.openImage(file.toString());
					if (imp == null) {
						log("  skipped (not an image): " + relative);
						failed++;
						continue;
					}
					ImagePlus result = s.segment(imp, settings);
					String name = file.getFileName().toString();
					int dot = name.lastIndexOf('.');
					Path target = output.resolve(relative).resolveSibling((dot > 0 ? name.substring(0, dot) : name) + "_seg.tif");
					Files.createDirectories(target.getParent());
					IJ.saveAsTiff(result, target.toString());
					done++;
					log("  " + relative + " -> " + output.relativize(target));
				} catch (Throwable e) {
					failed++;
					log("  failed: " + relative + ": " + Djl.message(e));
				}
			}
			log(String.format(Locale.ROOT, "Batch %s: %d segmented, %d failed or skipped, %.1f s", cancel ? "canceled" : "finished", done,
					failed, (System.nanoTime() - start) / 1e9));
			display.asyncExec(() -> {
				if (!progress.isDisposed()) {
					progress.setSelection(progress.getMaximum());
				}
			});
		});
	}

	private static boolean isImage(Path p) {
		String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
		for (String extension : IMAGE_EXTENSIONS) {
			if (name.endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	/* ------------------------------ Helpers of the view ------------------------------ */

	/* Runs a task in a background thread (the view is updated in the UI thread) */
	private void background(Runnable task) {
		Thread thread = new Thread(() -> {
			try {
				task.run();
			} finally {
				busy = false;
				display.asyncExec(this::updateButtons);
			}
		}, "Deep Learning Segmentation");
		thread.setDaemon(true);
		thread.start();
	}

	private void updateButtons() {
		if (loadButton == null || loadButton.isDisposed()) {
			return;
		}
		boolean ready = segmenter != null && !busy;
		loadButton.setEnabled(djl != null && !busy);
		segmentButton.setEnabled(ready);
		batchButton.setEnabled(ready);
		cancelButton.setEnabled(busy);
	}

	private void log(String text) {
		System.out.println("[Deep Learning Segmentation] " + text);
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if (!log.isDisposed()) {
				log.append(text + "\n");
			}
		});
	}

	private static Group group(Composite parent, String text, int columns) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(text);
		group.setLayout(new GridLayout(columns, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		return group;
	}

	private static Label label(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		return label;
	}

	private static Button button(Composite parent, String text, String tip) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setToolTipText(tip);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		return button;
	}

	private static Spinner spinner(Composite parent, int value, int min, int max, String tip) {
		Spinner spinner = new Spinner(parent, SWT.BORDER);
		spinner.setValues(value, min, max, 0, 1, 16);
		spinner.setToolTipText(tip);
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return spinner;
	}

	private static void folderButton(Composite parent, Text target, String tip) {
		Button button = button(parent, "Browse...", tip);
		button.addListener(SWT.Selection, _ -> {
			DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
			String folder = dialog.open();
			if (folder != null) {
				target.setText(folder);
			}
		});
	}

	/* ------------------------------ The segmentation with DJL ------------------------------ */

	/**
	 * A loaded model: segments an image (each slice of a stack). The image is
	 * converted to a float tensor [1, C, H, W] (padded or resized), the output
	 * (classes [1, K, H, W], one channel [1, 1, H, W] or labels [1, H, W]) is
	 * converted to a mask or a label image of the size of the image.
	 */
	static final class Segmenter implements AutoCloseable {

		/** The settings of the input. */
		static final class Settings {
			int channels = 1;
			int padMultiple = 16;
			int width, height;
			int normalization;
		}

		private final Djl djl;
		private final Object model;
		private final Object predictor;

		Segmenter(Djl djl, Path path, String engine) throws Exception {
			this.djl = djl;
			Class<?> ndList = djl.cls("ai.djl.ndarray.NDList");
			this.model = djl.inLoader(() -> {
				Object builder = djl.callStatic("ai.djl.repository.zoo.Criteria", "builder");
				builder = djl.call(builder, "setTypes", ndList, ndList);
				builder = djl.call(builder, "optModelPath", path);
				builder = djl.call(builder, "optEngine", engine);
				Object criteria = djl.call(builder, "build");
				return djl.call(criteria, "loadModel");
			});
			this.predictor = djl.inLoader(() -> djl.call(model, "newPredictor"));
		}

		/* The input shape declared by the model (e.g., [-1, 1, -1, -1]) or null */
		long[] inputShape() {
			try {
				Object pairs = djl.call(model, "describeInput");
				List<?> shapes = (List<?>) djl.call(pairs, "values");
				return shapes.isEmpty() ? null : (long[]) djl.call(shapes.get(0), "getShape");
			} catch (Throwable e) {
				return null;
			}
		}

		static String shapeText(long[] shape) {
			if (shape == null) {
				return "(not declared)";
			}
			StringBuilder b = new StringBuilder("[");
			for (int i = 0; i < shape.length; i++) {
				b.append(i > 0 ? ", " : "").append(shape[i] < 0 ? "?" : String.valueOf(shape[i]));
			}
			return b.append("]").toString();
		}

		/* Segments all slices of an image (the result: a mask 0/255 or a label image) */
		synchronized ImagePlus segment(ImagePlus imp, Settings settings) throws Exception {
			ImageStack stack = imp.getStack();
			ImageStack labels = new ImageStack(imp.getWidth(), imp.getHeight());
			int classes = 0;
			for (int slice = 1; slice <= stack.getSize(); slice++) {
				int[] result = { 0 };
				ImageProcessor label = segmentSlice(stack.getProcessor(slice), settings, result);
				classes = Math.max(classes, result[0]);
				labels.addSlice(stack.getSliceLabel(slice), label);
			}
			String title = imp.getTitle();
			int dot = title.lastIndexOf('.');
			ImagePlus result = new ImagePlus((dot > 0 ? title.substring(0, dot) : title) + "_segmentation", labels);
			result.setCalibration(imp.getCalibration().copy());
			result.setDimensions(imp.getNChannels(), imp.getNSlices(), imp.getNFrames());
			if (classes > 2) {
				result.setDisplayRange(0, classes - 1);
			}
			return result;
		}

		private ImageProcessor segmentSlice(ImageProcessor ip, Settings s, int[] classesOut) throws Exception {
			int w = ip.getWidth(), h = ip.getHeight();
			int channels = Math.max(1, s.channels);
			/* The size of the input: fixed (resized) or the image padded to a multiple */
			boolean resize = s.width > 0 && s.height > 0;
			int m = Math.max(1, s.padMultiple);
			int iw = resize ? s.width : (w + m - 1) / m * m;
			int ih = resize ? s.height : (h + m - 1) / m * m;
			float[][] planes = channels(ip, channels);
			if (resize) {
				for (int c = 0; c < channels; c++) {
					ImageProcessor f = new ij.process.FloatProcessor(w, h, planes[c]);
					f.setInterpolationMethod(ImageProcessor.BILINEAR);
					planes[c] = (float[]) f.resize(iw, ih).getPixels();
				}
			}
			normalize(planes, s.normalization, ip.getBitDepth());
			int pw = resize ? iw : w, ph = resize ? ih : h;
			float[] data = new float[channels * iw * ih];
			for (int c = 0; c < channels; c++) {
				for (int y = 0; y < ih; y++) {
					/* Padded with the mirrored image */
					int sy = mirror(y, ph);
					for (int x = 0; x < iw; x++) {
						data[(c * ih + y) * iw + x] = planes[c][sy * pw + mirror(x, pw)];
					}
				}
			}
			/* The prediction (the engine in the class loader of DJL) */
			Object[] output = djl.inLoader(() -> {
				Object manager = djl.call(djl.call(model, "getNDManager"), "newSubManager");
				try (AutoCloseable _ = (AutoCloseable) manager) {
					Object shape = djl.cls("ai.djl.ndarray.types.Shape").getConstructor(long[].class)
							.newInstance((Object) new long[] { 1, channels, ih, iw });
					Object array = djl.call(manager, "create", data, shape);
					@SuppressWarnings("unchecked")
					List<Object> input = (List<Object>) djl.cls("ai.djl.ndarray.NDList").getConstructor().newInstance();
					input.add(array);
					List<?> result = (List<?>) djl.call(predictor, "predict", input);
					Object out = result.get(0);
					long[] dims = (long[]) djl.call(djl.call(out, "getShape"), "getShape");
					String type = String.valueOf(djl.call(out, "getDataType"));
					float[] values;
					if (type.equalsIgnoreCase("float32")) {
						values = (float[]) djl.call(out, "toFloatArray");
					} else {
						Number[] numbers = (Number[]) djl.call(out, "toArray");
						values = new float[numbers.length];
						for (int i = 0; i < numbers.length; i++) {
							values[i] = numbers[i].floatValue();
						}
					}
					return new Object[] { dims, values };
				}
			});
			long[] dims = (long[]) output[0];
			float[] values = (float[]) output[1];
			/* The classes of the output: [1, K, H, W] (NCHW), [1, H, W, K] (NHWC) or [1, H, W] */
			int ow, oh, k;
			boolean nhwc = false;
			if (dims.length == 4 && dims[2] == ih && dims[3] == iw) {
				k = (int) dims[1];
				oh = ih;
				ow = iw;
			} else if (dims.length == 4 && dims[1] == ih && dims[2] == iw) {
				k = (int) dims[3];
				oh = ih;
				ow = iw;
				nhwc = true;
			} else if (dims.length == 3) {
				k = 1;
				oh = (int) dims[1];
				ow = (int) dims[2];
			} else {
				throw new IllegalStateException("An output of a segmentation is expected ([1, K, H, W] or [1, H, W]), not "
						+ shapeText(dims));
			}
			int[] label = new int[ow * oh];
			int classes = labels(values, k, ow * oh, nhwc, dims.length == 3, label);
			classesOut[0] = classes;
			/* A mask (0/255) for two classes, else the classes 0 - (K - 1) */
			ImageProcessor result = classes <= 2 ? new ByteProcessor(ow, oh) : classes < 256 ? new ByteProcessor(ow, oh)
					: new ShortProcessor(ow, oh);
			for (int i = 0; i < label.length; i++) {
				result.set(i % ow, i / ow, classes <= 2 ? (label[i] > 0 ? 255 : 0) : label[i]);
			}
			/* The size of the image again: crop the padding or resize back */
			if (resize) {
				result.setInterpolationMethod(ImageProcessor.NONE);
				return result.resize(w, h);
			}
			result.setRoi(0, 0, w, h);
			return result.crop();
		}

		/* The class of each pixel; returns the number of classes */
		private static int labels(float[] v, int k, int n, boolean nhwc, boolean labelOutput, int[] label) {
			if (k > 1) {
				for (int i = 0; i < n; i++) {
					int best = 0;
					float max = -Float.MAX_VALUE;
					for (int c = 0; c < k; c++) {
						float value = nhwc ? v[i * k + c] : v[c * n + i];
						if (value > max) {
							max = value;
							best = c;
						}
					}
					label[i] = best;
				}
				return k;
			}
			/* One channel: labels (integers) or a probability / logit (threshold) */
			boolean integers = labelOutput, logits = false;
			int maxLabel = 0;
			for (int i = 0; i < n; i++) {
				if (v[i] != Math.rint(v[i])) {
					integers = false;
				}
				if (v[i] < 0 || v[i] > 1) {
					logits = true;
				}
			}
			for (int i = 0; i < n; i++) {
				label[i] = integers ? (int) v[i] : (logits ? v[i] > 0 : v[i] > 0.5f) ? 1 : 0;
				maxLabel = Math.max(maxLabel, label[i]);
			}
			return Math.max(2, maxLabel + 1);
		}

		/* The channels of the input (gray: the values, RGB: the luminance or R, G, B) */
		private static float[][] channels(ImageProcessor ip, int channels) {
			int n = ip.getWidth() * ip.getHeight();
			float[][] planes = new float[channels][];
			if (ip instanceof ColorProcessor color && channels >= 3) {
				byte[] r = new byte[n], g = new byte[n], b = new byte[n];
				color.getRGB(r, g, b);
				byte[][] rgb = { r, g, b };
				for (int c = 0; c < channels; c++) {
					planes[c] = new float[n];
					for (int i = 0; i < n; i++) {
						planes[c][i] = rgb[Math.min(c, 2)][i] & 0xff;
					}
				}
				return planes;
			}
			float[] gray = (float[]) ip.convertToFloatProcessor().getPixels();
			for (int c = 0; c < channels; c++) {
				planes[c] = c == 0 ? gray : gray.clone();
			}
			return planes;
		}

		private static void normalize(float[][] planes, int normalization, int bitDepth) {
			switch (normalization) {
			case 0 -> {
				/* Min-Max of all channels to 0 - 1 */
				float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
				for (float[] p : planes) {
					for (float v : p) {
						min = Math.min(min, v);
						max = Math.max(max, v);
					}
				}
				float range = max > min ? max - min : 1;
				for (float[] p : planes) {
					for (int i = 0; i < p.length; i++) {
						p[i] = (p[i] - min) / range;
					}
				}
			}
			case 1 -> {
				for (float[] p : planes) {
					for (int i = 0; i < p.length; i++) {
						p[i] /= 255f;
					}
				}
			}
			case 2 -> {
				/* ImageNet: 0 - 1 (8-bit and RGB: / 255, else Min-Max), then (v - mean) / std of R, G, B */
				if (bitDepth == 8 || bitDepth == 24) {
					normalize(planes, 1, bitDepth);
				} else {
					normalize(planes, 0, bitDepth);
				}
				float[] mean = { 0.485f, 0.456f, 0.406f }, std = { 0.229f, 0.224f, 0.225f };
				for (int c = 0; c < planes.length; c++) {
					for (int i = 0; i < planes[c].length; i++) {
						planes[c][i] = (planes[c][i] - mean[Math.min(c, 2)]) / std[Math.min(c, 2)];
					}
				}
			}
			default -> {
				/* None: the values of the image */
			}
			}
		}

		/* A mirrored index of the padding (0 ... n - 1) */
		private static int mirror(int i, int n) {
			if (n == 1) {
				return 0;
			}
			int period = 2 * (n - 1);
			i = Math.floorMod(i, period);
			return i < n ? i : period - i;
		}

		@Override
		public void close() {
			for (Object o : new Object[] { predictor, model }) {
				try {
					if (o instanceof AutoCloseable c) {
						c.close();
					}
				} catch (Exception e) {
					/* Closed */
				}
			}
		}
	}

	/* ------------------------------ The Deep Java Library with reflection ------------------------------ */

	/**
	 * Calls the Deep Java Library with reflection: the example compiles and runs
	 * without the library (and shows how to install it). The calls run with the
	 * class loader of DJL as context class loader (DJL finds its engines with the
	 * ServiceLoader).
	 */
	static final class Djl {

		private final ClassLoader loader;

		private Djl(ClassLoader loader) {
			this.loader = loader;
		}

		/* DJL from the class loader of the example (the compiler libraries), else the context class loader */
		static Djl find(ClassLoader exampleLoader) {
			for (ClassLoader loader : new ClassLoader[] { exampleLoader, Thread.currentThread().getContextClassLoader() }) {
				if (loader == null) {
					continue;
				}
				try {
					Class.forName("ai.djl.repository.zoo.Criteria", false, loader);
					return new Djl(loader);
				} catch (ClassNotFoundException | LinkageError e) {
					/* Not in this class loader */
				}
			}
			return null;
		}

		Class<?> cls(String name) throws ClassNotFoundException {
			return Class.forName(name, true, loader);
		}

		/* The version of DJL */
		String version() {
			try {
				return String.valueOf(inLoader(() -> callStatic("ai.djl.engine.Engine", "getDjlVersion")));
			} catch (Throwable e) {
				return "(unknown version)";
			}
		}

		/* The names of the engines which can be loaded */
		@SuppressWarnings("unchecked")
		Set<String> engines() {
			try {
				return new java.util.TreeSet<>((Set<String>) inLoader(() -> callStatic("ai.djl.engine.Engine", "getAllEngines")));
			} catch (Throwable e) {
				return Set.of();
			}
		}

		/* Runs a task with the class loader of DJL as context class loader */
		<T> T inLoader(Callable<T> task) throws Exception {
			Thread thread = Thread.currentThread();
			ClassLoader old = thread.getContextClassLoader();
			thread.setContextClassLoader(loader);
			try {
				return task.call();
			} finally {
				thread.setContextClassLoader(old);
			}
		}

		Object callStatic(String className, String name, Object... args) throws Exception {
			return invoke(cls(className), null, name, args);
		}

		Object call(Object target, String name, Object... args) throws Exception {
			return invoke(target.getClass(), target, name, args);
		}

		private static Object invoke(Class<?> type, Object target, String name, Object[] args) throws Exception {
			for (Method m : type.getMethods()) {
				if (!m.getName().equals(name) || m.getParameterCount() != args.length || (target == null) != Modifier.isStatic(m.getModifiers())) {
					continue;
				}
				Class<?>[] p = m.getParameterTypes();
				boolean fits = true;
				for (int i = 0; i < p.length && fits; i++) {
					fits = fits(p[i], args[i]);
				}
				if (fits) {
					/* Public methods of classes which are not public (e.g., the implementation of an engine) */
					m.setAccessible(true);
					try {
						return m.invoke(target, args);
					} catch (java.lang.reflect.InvocationTargetException e) {
						Throwable cause = e.getCause();
						if (cause instanceof Exception ex) {
							throw ex;
						}
						throw new RuntimeException(cause);
					}
				}
			}
			throw new NoSuchMethodException(type.getName() + "." + name + " with " + args.length + " arguments");
		}

		private static boolean fits(Class<?> p, Object arg) {
			if (arg == null) {
				return !p.isPrimitive();
			}
			if (p.isPrimitive()) {
				return (p == int.class && arg instanceof Integer) || (p == long.class && arg instanceof Long)
						|| (p == float.class && arg instanceof Float) || (p == double.class && arg instanceof Double)
						|| (p == boolean.class && arg instanceof Boolean);
			}
			return p.isInstance(arg);
		}

		/* The message of an error (the cause of a reflection error) */
		static String message(Throwable e) {
			Throwable t = e;
			while ((t instanceof java.lang.reflect.InvocationTargetException || t instanceof RuntimeException) && t.getCause() != null
					&& t.getCause() != t) {
				t = t.getCause();
			}
			return t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
		}
	}
}
