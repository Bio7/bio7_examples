package opengl.plotapi.dynamic;

import java.util.Random;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A dynamic rose chart: a wind rose which accumulates the measurements of a
 * simulated weather station (each step adds an hour with a direction and a
 * speed, the prevailing wind turns slowly). The rose is updated in each step
 * of the model.
 */
public class DynamicRoseModel extends com.eco.bio7.compile.Model {

	private final String[] directions = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW" };
	private final Random random = new Random(9);
	private double[][] counts;
	private int hours;
	private Plot3D plot;

	public DynamicRoseModel() {
		setup();
	}

	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		counts = new double[16][3];
		hours = 0;
		plot = new Plot3D();
		layers().show();
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	private Plot3D layers() {
		/* The frequencies in percent */
		double[][] percent = new double[16][3];
		for (int s = 0; s < 16; s++) {
			for (int k = 0; k < 3; k++) {
				percent[s][k] = hours > 0 ? 100 * counts[s][k] / hours : 0;
			}
		}
		return plot.title("Wind Rose\n" + hours + " hours").clearLayers()
				.add(Plot3D.rose(percent, directions).classes("0 - 3 m/s", "3 - 8 m/s", "> 8 m/s").colors(0x9ECAE1, 0x4292C6, 0x08306B));
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (!SpatialUtil.canStep()) {
			return;
		}
		/* 12 hours in each step */
		for (int h = 0; h < 12; h++) {
			hours++;
			double prevailing = 225 + 60 * Math.sin(hours / 400.0);
			double angle = (prevailing + 40 * random.nextGaussian() + 360) % 360;
			int sector = (int) Math.round(angle / 22.5) % 16;
			double speed = Math.abs(4 + 3 * random.nextGaussian());
			counts[sector][speed < 3 ? 0 : speed < 8 ? 1 : 2]++;
		}
		layers().update();
	}

	public void close() {
		Plot3D.clear();
	}
}
