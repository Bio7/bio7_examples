/*
This Snippet prints the selected values in the acive sheet!
*/
String []values=Bio7Grid.getSelection();
for (int i = 0; i < values.length; i++) {
	System.out.println(values[i])
}