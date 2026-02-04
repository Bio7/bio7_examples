/*Eclipse Terminal simplified API example.*/
import com.eco.bio7.console.ShellUtil;

String title="List Directory";//For a new process change the title!
String processPath="/bin/bash";
String command="""
-c 'nano; exec bash'
""";

ShellUtil shellUtil=new ShellUtil()
shellUtil.execShellCommand(title,processPath,command);
Thread.sleep(5000);
shellUtil.closeProcess();

title="Process";//For a new process change the title!
processPath="/bin/bash";
command="""
-c 'top; exec bash'
""";

shellUtil=new ShellUtil()
shellUtil.execShellCommand(title,processPath,command);
Thread.sleep(5000);
shellUtil.closeProcess();

/*Closes the view with the specified id.*/
Thread.sleep(1000);
Work.closeView("org.eclipse.terminal.view.ui.TerminalsView");