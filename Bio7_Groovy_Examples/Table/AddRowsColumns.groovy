/*Bio7 API to add rows and columns to a Bio7 Table sheet scripted with Groovy!*/

import org.eclipse.nebula.widgets.grid.Grid;

Grid grid=Bio7Grid.getGrid();

Bio7Grid.createSheet(0,0,"Values");//Create sheet with 0 columns and 0 rows!
for (int i = 0; i < 10; i++) {
	
	Bio7Grid.createColumn(1,70,""+i);// Add 10 columns with width = 70 pixels!
}

Bio7Grid.createRow(0,20);//Add one row with height = 20 pixels!

Bio7Grid.setValue(0,0, "1200");//Set a value at position !