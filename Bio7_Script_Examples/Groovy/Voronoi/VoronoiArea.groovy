/*
This script transfers all active voronoi areas (if available!) in the Points panel 
to a sheet of the Bio7 Table view!
*/

import com.vividsolutions.jts.geom.Geometry;

Geometry g = PointPanel.getGeomVoronoi();
int count = g.getNumGeometries();
Bio7Grid.createSheet(1, count, "Voronoi Areas");
String[][] val = new String[count][1];
for (int i = 0; i < count; i++) {
	Geometry geom = g.getGeometryN(i);
	val[i][0] = geom.getArea().toString();

}

Bio7Grid.setValues(val);	

