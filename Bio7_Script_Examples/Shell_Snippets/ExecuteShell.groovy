/*Eclipse Terminal simplified API example with shell properties.*/
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.terminal.view.core.ITerminalService;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import com.eco.bio7.console.*;


Map<String, Object> properties = new HashMap<>();
/*For a new shell change the title (ITerminalsConnectorConstants.PROP_TITLE); */
properties.put(ITerminalsConnectorConstants.PROP_TITLE, "Bio7 Console");
properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, "/bin/bash");
// Use -c to pass the command string, ending with 'exec bash' to keep it open
// Note: Semi-colon separates commands on Linux
// We use here Java Text Blocks for readable multiline strings.
properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, 
"""
-c 'nano; exec bash'
""");

new ShellUtil().execShellCommand(properties);

