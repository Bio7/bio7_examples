package gdal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

/*
 * Code written for the old Java bindings of GDAL (gdal.jar, JNI) runs with the
 * compatibility layer of the GDAL plugin (the classes of org.gdal on the FFM
 * bridge): here the classic style of the GDAL documentation.
 * Run Gdal01_RasterBasics first (it creates dem_utm.tif).
 */
public class Gdal06_CompatibilityAPI {

	public static void main(String[] args) throws Exception {
		if (!GdalSetup.load()) {
			return;
		}
		Path dem = GdalSetup.folder().resolve("dem_utm.tif");
		if (!Files.exists(dem)) {
			System.err.println("Please run Gdal01_RasterBasics first.");
			return;
		}
		System.out.println("GDAL " + gdal.VersionInfo("RELEASE_NAME") + ", " + gdal.GetDriverCount() + " drivers");
		Dataset ds = gdal.Open(dem.toString(), gdalconst.GA_ReadOnly);
		if (ds == null) {
			System.err.println("Not opened: " + gdal.GetLastErrorMsg());
			return;
		}
		System.out.println("Driver: " + ds.GetDriver().getShortName() + ", size " + ds.getRasterXSize() + " x " + ds.getRasterYSize());
		double[] gt = new double[6];
		ds.GetGeoTransform(gt);
		System.out.println("Origin " + gt[0] + " / " + gt[3] + ", pixel " + gt[1] + " x " + gt[5]);

		/* The pixels of a band in a direct buffer */
		Band band = ds.GetRasterBand(1);
		int w = band.getXSize(), h = band.getYSize();
		ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
		band.ReadRaster_Direct(0, 0, w, h, w, h, gdalconst.GDT_Float32, buf);
		float max = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < w * h; i++) {
			max = Math.max(max, buf.getFloat(i * 4));
		}
		System.out.println("Highest point: " + max + " m");

		/* The coordinate system and a transformation (x = longitude) */
		SpatialReference srs = new SpatialReference(ds.GetProjectionRef());
		SpatialReference wgs84 = new SpatialReference();
		wgs84.ImportFromEPSG(4326);
		CoordinateTransformation ct = new CoordinateTransformation(srs, wgs84);
		double[] p = ct.TransformPoint(gt[0], gt[3]);
		System.out.println(srs.GetAttrValue("PROJCS") + ": the upper left corner at lon " + p[0] + ", lat " + p[1]);
		System.out.println("PROJ: " + srs.ExportToProj4());
		ct.delete();
		srs.delete();
		wgs84.delete();
		ds.delete();
	}
}
