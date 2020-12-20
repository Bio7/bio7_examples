/*
This Snippet prints the values of the current sheet!
*/
String[][]val=Bio7Grid.getValues();
for (int i = 0; i < val.length; i++) {
	for (int u = 0; u < val[0].length; u++) {
		System.out.println(val[i][u]);
	}
}