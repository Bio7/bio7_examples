package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * A rose chart (a wind rose): the frequency of the wind directions (16
 * sectors) with three classes of the wind speed stacked from the center. The
 * export action of the 3D view is enabled (HTML or Blender).
 */
public class RosePlotApi {

	public static void main(String[] args) {
		String[] directions = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW" };
		double[][] frequency = new double[16][3];
		for (int s = 0; s < 16; s++) {
			/* Mostly from the west and the south west */
			double angle = 2 * Math.PI * s / 16;
			double west = Math.exp(Math.cos(angle - 1.5 * Math.PI) * 1.2);
			frequency[s][0] = 1.5 + 0.8 * west;
			frequency[s][1] = 0.8 + 1.1 * west;
			frequency[s][2] = 0.2 + 0.7 * west * west / 3;
		}
		new Plot3D().title("Wind Rose\nFrequency of the wind directions [%]")
				.add(Plot3D.rose(frequency, directions).classes("0 - 3 m/s", "3 - 8 m/s", "> 8 m/s").colors(0x9ECAE1, 0x4292C6, 0x08306B))
				.exportAction(true)
				.show();
	}
}
