/*
This Snippet replaces all points with a comma!
*/
String[][]val=Bio7Grid.getValues();
for (int i = 0; i < val.length; i++) {
	for (int u = 0; u < val[0].length; u++) { 
		  val[i][u]=val[i][u].replace(".",",");
          }
     }
Bio7Grid.setValues(val);