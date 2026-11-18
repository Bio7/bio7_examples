/*
A new Bio7 API method to get selected workspace values (names) of the R shell!
*/
String []names=RTools.getShellSelections();
for (int i = 0; i < names.length; i++) {
	System.out.println(names[i]);
}