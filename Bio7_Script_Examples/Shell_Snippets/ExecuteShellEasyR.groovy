/*Eclipse Terminal simplified API example.*/

import com.eco.bio7.console.ShellUtil;

String title="R";//For a new process change the title!
String processPath="/usr/local/bin/r";
String command="""
-e 'print(summary(runif(100)))'
""";
ShellUtil shellUtil=new ShellUtil()
shellUtil.execShellCommand(title,processPath,command);
//Thread.sleep(5000);
//shellUtil.closeProcess();

/*Closes the view with the specified id.*/
//Thread.sleep(3000);
//Work.closeView("org.eclipse.terminal.view.ui.TerminalsView");