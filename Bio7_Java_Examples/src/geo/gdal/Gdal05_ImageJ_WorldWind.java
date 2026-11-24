package geo.gdal;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.widgets.Display;

import com.eco.bio7.geo.GdalImageJ;
import com.eco.bio7.geo.GdalWorldWind;

import ij.ImagePlus;

/*
 * GDAL in ImageJ and WorldWind from Java (as File > GDAL in the menu):
 * the elevation model and the hillshade as ImageJ images (calibrated in
 * meters), the plots as ROIs, the hillshade and the plots as WorldWind layers.
 * Run Gdal01, Gdal03 and Gdal04 first. Open the WorldWind perspective for the
 * WorldWind part.
 *
 *   GdalImageJ.open(file)                     an ImageJ image (any raster format)
 *   GdalImageJ.exportGeoTiff(imp, file)       an ImageJ image as GeoTIFF
 *   GdalImageJ.addVectorRois(file, imp, rm)   vectors as ROIs of the image
 *   GdalWorldWind.addRaster(file) / addVector(file)
 */
public class Gdal05_ImageJ_WorldWind {

	public static void main(String[] args) throws Exception {
		if (!GdalSetup.load()) {
			return;
		}
		Path dir = GdalSetup.folder();
		Path dem = dir.resolve("dem_utm.tif"), shade = dir.resolve("hillshade.tif"), plots = dir.resolve("plots.geojson");
		if (!Files.exists(dem) || !Files.exists(shade) || !Files.exists(plots)) {
			System.err.println("Please run Gdal01_RasterBasics, Gdal03_Vectors and Gdal04_TerrainAnalysis first.");
			return;
		}

		/* ImageJ: the images are shown in the user interface thread */
		ImagePlus demImage = GdalImageJ.open(dem);
		ImagePlus shadeImage = GdalImageJ.open(shade);
		Display.getDefault().syncExec(() -> {
			demImage.show();
			shadeImage.show();
			/* The plots (WGS84) as ROIs of the hillshade (UTM) */
			ij.plugin.frame.RoiManager rm = ij.plugin.frame.RoiManager.getInstance2() != null ? ij.plugin.frame.RoiManager.getInstance2()
					: new ij.plugin.frame.RoiManager();
			int n = GdalImageJ.addVectorRois(plots, shadeImage, rm);
			rm.runCommand(shadeImage, "Show All");
			System.out.println("ROIs: " + n);
		});
		System.out.println("Calibration: 1 pixel = " + demImage.getCalibration().pixelWidth + " " + demImage.getCalibration().getUnit());

		/* An ImageJ result back as GeoTIFF (with the georeferencing of the source) */
		ImagePlus smooth = demImage.duplicate();
		smooth.getProcessor().blurGaussian(3);
		GdalImageJ.exportGeoTiff(smooth, dir.resolve("dem_smooth.tif"));
		System.out.println("Written: " + dir.resolve("dem_smooth.tif"));

		/* WorldWind (if the view is open): reprojected to WGS84 by GDAL */
		try {
			GdalWorldWind.addRaster(shade);
			System.out.println("WorldWind: " + GdalWorldWind.addVector(plots) + " plots");
		} catch (RuntimeException e) {
			System.out.println("WorldWind: " + e.getMessage());
		}
	}
}
