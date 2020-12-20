/*
An example to measure ImageJ particle attributes with Java and add the values to a Bio7 Table sheet!
*/
package imagej;

import com.eco.bio7.rbridge.Bio7Grid;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.ParticleAnalyzer;

public class ParticleAnalyzerNotTableOpen implements PlugIn {

	private int min;
	private int max;
	private int options;
	private int measurements;
	private ImagePlus imp;

	public void run(String arg) {
		imp = IJ.openImage("http://wsr.imagej.net/images/particles.gif");
		imp.show();
		min = 0;
		max = 99999;
		options = ParticleAnalyzer.SHOW_NONE;
		measurements = Measurements.AREA + Measurements.CENTROID + Measurements.SLICE;

		ResultsTable rt = new ResultsTable();

		ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, min, max);
		pa.analyze(imp);

		// We add an additional row for the column title!
		String[][] values = new String[rt.size() + 1][2];
		values[0][0] = "Area";
		values[0][1] = "Slice";
		for (int i = 0; i < rt.size(); i++) {
			double area = rt.getValue("Area", i);
			double slice = rt.getValue("Slice", i);
			values[i + 1][0] = Double.toString(area);
			values[i + 1][1] = Double.toString(slice);
		}
		Bio7Grid.setValues(values, "Particle Analysis");
	}
}