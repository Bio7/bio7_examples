package python.camera;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.pybridge.PythonUtil;
import com.eco.bio7.pyffm.PyValue;

import gui.BackgroundJob;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Plot;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import python.PythonExamples;

/*
 * Records the camera (webcam) and analyzes each frame while recording: a Java
 * macro (the method javaMacro below: fast, edit it for your own measurements)
 * or an ImageJ macro (written in the GUI). The measurements of each frame go
 * into a results table (with the time) and are plotted at the end; the frames
 * become an ImageJ stack (optional) and are shown live (optional).
 *
 * The frames come from OpenCV in the embedded Python (camera_capture.py in
 * Bio7_Script_Examples/Python/OpenCV), each frame is an ImageJ processor in
 * Java. A video file can be used instead of the camera (Source: its path).
 *
 * The GUI is in CameraRecorderGui (WindowBuilder: Open With > WindowBuilder Editor).
 * Requires: the embedded Python with opencv-python-headless (Python Packages: "OpenCV").
 * macOS: Bio7 needs the permission for the camera (System Settings > Privacy & Security > Camera).
 */
public class CameraRecorder extends com.eco.bio7.compile.Model {

	/** The settings of a recording (from the GUI). analysis: 0 none, 1 Java macro, 2 ImageJ macro. */
	public record Settings(String source, int width, int height, boolean gray, int seconds, int maxFrames, boolean preview, int previewEvery, int analysis,
			String imagejMacro, boolean keepStack) {
	}

	private CameraRecorderGui gui;
	private volatile boolean recording;

	public CameraRecorder() {
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Camera Recorder");
			parent.setLayout(new FillLayout());
			gui = new CameraRecorderGui(parent, CameraRecorder.this, SWT.NONE);
			parent.layout(true);
			/* Closing the view ends a recording (the camera is released) */
			gui.addDisposeListener(e -> recording = false);
		});
	}

	/* ---------------- The Java macro: called for each frame ---------------- */

	private ImageProcessor previous;

	/**
	 * The Java macro: called for each frame while recording, returns the
	 * measurements of the frame (name -> value; the columns of the results table).
	 * Edit it for your own measurements (and run this class again). It must be
	 * fast: at 30 frames per second there are about 30 ms per frame.
	 *
	 * @param ip      the frame (RGB: ColorProcessor, gray: ByteProcessor)
	 * @param frame   the number of the frame (from 1)
	 * @param seconds the time since the start of the recording
	 */
	Map<String, Double> javaMacro(ImageProcessor ip, int frame, double seconds) {
		Map<String, Double> m = new LinkedHashMap<>();
		ImageProcessor gray = ip instanceof ColorProcessor ? ip.convertToByte(false) : ip;
		ImageStatistics st = gray.getStats();
		m.put("Mean", st.mean);
		m.put("StdDev", st.stdDev);

		/* Motion: the share of the pixels which changed by more than 25 gray values since the last frame */
		byte[] now = (byte[]) gray.getPixels();
		double motion = 0;
		if (previous != null && previous.getPixelCount() == now.length) {
			byte[] before = (byte[]) previous.getPixels();
			int changed = 0;
			for (int i = 0; i < now.length; i++) {
				if (Math.abs((now[i] & 0xff) - (before[i] & 0xff)) > 25) {
					changed++;
				}
			}
			motion = 100.0 * changed / now.length;
		}
		m.put("Motion %", motion);

		/* Bright areas: the share of the pixels brighter than the mean + 2 standard deviations */
		double limit = st.mean + 2 * st.stdDev;
		int bright = 0;
		for (byte b : now) {
			if ((b & 0xff) > limit) {
				bright++;
			}
		}
		m.put("Bright %", 100.0 * bright / now.length);
		previous = gray == ip ? gray.duplicate() : gray;
		return m;
	}

	/* ---------------- The ImageJ macro ---------------- */

	/**
	 * Runs the ImageJ macro on the frame (its current image, in batch mode, not
	 * shown). Its return value are the measurements: "name=value,name=value" or
	 * numbers "1.5,2" (named "Macro 1", "Macro 2", ...).
	 */
	private Map<String, Double> imagejMacro(String code, ImageProcessor ip, int frame) {
		ImagePlus imp = new ImagePlus("frame " + frame, ip);
		WindowManager.setTempCurrentImage(imp);
		String ret = new Interpreter().run(code, String.valueOf(frame));
		WindowManager.setTempCurrentImage(null);
		Map<String, Double> m = new LinkedHashMap<>();
		if (ret == null || ret.isBlank()) {
			return m;
		}
		String[] parts = ret.split("[,;]");
		for (int i = 0; i < parts.length; i++) {
			String p = parts[i].trim();
			int eq = p.indexOf('=');
			String name = eq > 0 ? p.substring(0, eq).trim() : "Macro " + (i + 1);
			try {
				m.put(name, Double.parseDouble((eq > 0 ? p.substring(eq + 1) : p).trim()));
			} catch (NumberFormatException e) {
				/* not a number: skipped */
			}
		}
		return m;
	}

	/* ---------------- The recording ---------------- */

	public void start(Settings s) {
		if (recording) {
			return;
		}
		recording = true;
		previous = null;
		gui.setRecording(true);
		BackgroundJob.run("Camera recording", () -> record(s), status -> {
			recording = false;
			if (!gui.isDisposed()) {
				gui.setRecording(false);
				gui.setStatus(status);
			}
		});
	}

	public void stop() {
		recording = false;
	}

	private String record(Settings s) throws Exception {
		String source = s.source().trim().matches("\\d+") ? s.source().trim() : PythonExamples.quote(s.source().trim());
		PythonUtil.exec(PythonExamples.importModule(PythonExamples.folder("OpenCV"), "camera_capture", "_cam")
				+ String.format(Locale.ROOT, "_cam_info = _cam.open_camera(%s, %d, %d)", source, s.width(), s.height()));
		String grab = "_cam.grab(" + (s.gray() ? "True" : "False") + ")";
		ResultsTable rt = new ResultsTable();
		ImageStack stack = null;
		List<Double> times = new ArrayList<>();
		ImagePlus live = null;
		boolean batch = Interpreter.batchMode;
		if (s.analysis() == 2) {
			Interpreter.batchMode = true;
		}
		long t0 = System.nanoTime();
		long analysisNanos = 0;
		int frame = 0;
		try {
			while (recording && frame < s.maxFrames() && (System.nanoTime() - t0) / 1e9 < s.seconds()) {
				PyValue v = PythonUtil.eval(grab);
				if (!(v instanceof PyValue.Array a)) {
					break;   // the end of a video file
				}
				double t = (System.nanoTime() - t0) / 1e9;
				ImageProcessor ip = processor(a);
				frame++;
				times.add(t);

				/* The analysis of the frame */
				Map<String, Double> m = null;
				long a0 = System.nanoTime();
				if (s.analysis() == 1) {
					m = javaMacro(ip, frame, t);
				} else if (s.analysis() == 2) {
					m = imagejMacro(s.imagejMacro(), ip, frame);
				}
				analysisNanos += System.nanoTime() - a0;
				if (m != null) {
					rt.incrementCounter();
					rt.addValue("Frame", frame);
					rt.addValue("Time (s)", t);
					for (Map.Entry<String, Double> e : m.entrySet()) {
						rt.addValue(e.getKey(), e.getValue());
					}
				}

				if (s.keepStack()) {
					if (stack == null) {
						stack = new ImageStack(ip.getWidth(), ip.getHeight());
					}
					stack.addSlice(String.format(Locale.ROOT, "%.3f s", t), ip);
				}
				/* The live view (every n-th frame) */
				if (s.preview() && frame % Math.max(1, s.previewEvery()) == 1 % Math.max(1, s.previewEvery())) {
					if (live == null || live.getWindow() == null || live.getWidth() != ip.getWidth()) {
						ImagePlus created = new ImagePlus("Camera (live)", ip);
						Display.getDefault().syncExec(created::show);
						live = created;
					} else {
						live.setProcessor(ip);
						live.updateAndDraw();
					}
				}
				/* The GUI: frames, rate and the last measurements (5 times per second) */
				if (frame % 6 == 0) {
					String text = String.format(Locale.ROOT, "Recording: %d frames, %.1f s, %.1f frames/s%s", frame, t, frame / Math.max(t, 1e-3), values(m));
					Display.getDefault().asyncExec(() -> gui.setStatus(text));
				}
			}
		} finally {
			Interpreter.batchMode = batch;
			PythonUtil.exec("_cam.close()");
		}
		if (frame == 0) {
			return "No frames were recorded";
		}
		double elapsed = times.get(times.size() - 1);
		double rate = frame > 1 ? (frame - 1) / elapsed : 0;

		/* The results: the stack (with the frame rate), the table and a plot of the measurements */
		ImagePlus recorded = null;
		if (stack != null) {
			recorded = new ImagePlus("Camera recording (" + frame + " frames)", stack);
			recorded.setDimensions(1, 1, frame);
			Calibration cal = recorded.getCalibration();
			cal.fps = rate;
			cal.frameInterval = rate > 0 ? 1 / rate : 0;
			cal.setTimeUnit("s");
		}
		Plot plot = rt.size() > 1 ? plot(rt) : null;
		ImagePlus shown = recorded;
		Display.getDefault().syncExec(() -> {
			if (shown != null) {
				shown.show();
			}
			if (rt.size() > 0) {
				rt.show("Camera measurements");
			}
			if (plot != null) {
				plot.show();
			}
		});
		return String.format(Locale.ROOT, "Recorded %d frames in %.1f s = %.1f frames/s; analysis %.2f ms per frame%s", frame, elapsed, rate, analysisNanos / 1e6 / frame,
				stack != null ? String.format(Locale.ROOT, ", stack %.0f MB", (double) stack.getSize() * stack.getWidth() * stack.getHeight() * (s.gray() ? 1 : 4) / 1048576) : "");
	}

	/* A numpy frame (gray (h, w) or RGB (h, w, 3) uint8) as ImageJ processor */
	private static ImageProcessor processor(PyValue.Array a) {
		int[] shape = a.shape();
		int h = shape[0], w = shape[1];
		byte[] d = (byte[]) a.data();
		if (shape.length == 2) {
			return new ByteProcessor(w, h, d);
		}
		int[] px = new int[w * h];
		for (int i = 0, j = 0; i < px.length; i++, j += 3) {
			px[i] = 0xff000000 | (d[j] & 0xff) << 16 | (d[j + 1] & 0xff) << 8 | (d[j + 2] & 0xff);
		}
		return new ColorProcessor(w, h, px);
	}

	/* The measurements over the time (each column scaled to its range: one plot for all) */
	private static Plot plot(ResultsTable rt) {
		double[] t = rt.getColumn("Time (s)");
		Plot plot = new Plot("Camera measurements over time", "Time (s)", "Value (scaled to 0..1 per measurement)");
		java.awt.Color[] colors = { new java.awt.Color(76, 114, 176), new java.awt.Color(196, 78, 82), new java.awt.Color(85, 168, 104), new java.awt.Color(221, 132, 82),
				new java.awt.Color(129, 114, 179) };
		int k = 0;
		StringBuilder legend = new StringBuilder();
		for (String c : rt.getHeadings()) {
			if (c.equals("Frame") || c.equals("Time (s)") || c.equals("Label") || !rt.columnExists(c)) {
				continue;
			}
			double[] v = rt.getColumn(c);
			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			for (double x : v) {
				min = Math.min(min, x);
				max = Math.max(max, x);
			}
			double range = max > min ? max - min : 1;
			double[] scaled = new double[v.length];
			for (int i = 0; i < v.length; i++) {
				scaled[i] = (v[i] - min) / range;
			}
			plot.setColor(colors[k % colors.length]);
			plot.setLineWidth(1.5f);
			plot.add("line", t, scaled);
			legend.append(String.format(Locale.ROOT, "%s (%.3g - %.3g)%n", c, min, max));
			k++;
		}
		plot.setLimits(t[0], t[t.length - 1], -0.05, 1.05);
		if (k > 0) {
			plot.addLegend(legend.toString().trim());
		}
		return plot;
	}

	private static String values(Map<String, Double> m) {
		if (m == null || m.isEmpty()) {
			return "";
		}
		StringBuilder b = new StringBuilder(" | ");
		m.forEach((k, v) -> b.append(k).append(' ').append(String.format(Locale.ROOT, "%.2f", v)).append("  "));
		return b.toString();
	}
}
