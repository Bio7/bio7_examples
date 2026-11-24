/*
A more complex TM Terminal example for MacOSX and Linux.
The results of a top bash command is written to a file and imported
to a Bio7 Table Sheet (open the view to see the result).
*/
import com.eco.bio7.console.ShellUtil;
import java.io.File;

// Create a snapshot of 'top' on the Mac
String tempPath = System.getProperty("user.home") + "/top_colored.txt";
String command = "-c \"top -l 1 -n 35 > " + tempPath + "; exit\"";

ShellUtil shellUtil = new ShellUtil();
shellUtil.execShellCommand("Top Snapshot", "/bin/bash", command);

// Control: Wait for the file (max. 10 seconds)
File file = new File(tempPath);
int maxAttempts = 50; 
int attempt = 0;
boolean fileReady = false;

while (attempt < maxAttempts) {
    if (file.exists() && file.length() > 0) {
        fileReady = true;
        break; 
    }
    Thread.sleep(200); 
    attempt++;
}

if (!fileReady) {
    println "Error: File not found or empty after 10 seconds.";
    return; 
}

// Read file and find the header line
List<String> allLines = file.readLines();
int headerIndex = allLines.findIndexOf { it.contains("PID    COMMAND") };
if (headerIndex == -1) headerIndex = 0;

String headerLine = allLines[headerIndex].trim();
String[] headerNames = headerLine.split(/\s+/);
int cols = headerNames.length; 
int rows = 35;

// Initialize Bio7 Grid
Bio7Grid.createSheet(0, 0, "System_Monitor_Blue");

for (int j = 0; j < cols; j++) {
    int width = (j == 1) ? 350 : Math.max(80, headerNames[j].length() * 12);
    Bio7Grid.createColumn(j, width, headerNames[j]); 
}

// Fill data and alternate column colors
String[][] val = new String[rows][cols];

for (int i = 0; i < rows && (headerIndex + i) < allLines.size(); i++) {
    Bio7Grid.createRow(i, 20);
    /*Init color workaround!*/ 
    Bio7Grid.setColor(0, 0, 173, 216, 230);
    String line = allLines[headerIndex + i].trim();
    String[] parts = line.split(/\s+/, cols);

    for (int j = 0; j < cols; j++) {
        val[i][j] = (j < parts.length) ? parts[j] : "";
        
        // Alternate colors: Even columns light blue, odd columns white
        // This ensures cell (0,0) is also colored
        if (j % 2 == 0) {
            Bio7Grid.setColor(i, j, 173, 216, 230); // Light Blue (RGB)
        } else {
            Bio7Grid.setColor(i, j, 255, 255, 255); // White
        }
    }
}

// 6. Set values and clean up
Bio7Grid.setValues(val);
file.delete();
println "Process list with light blue alternating columns loaded as a Table view Sheet.";
/*Closes the view with the specified id.*/
Thread.sleep(1000);
Work.closeView("org.eclipse.terminal.view.ui.TerminalsView");