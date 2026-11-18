package html;

import java.io.File;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports the epidemic model of the JavaScript file Epidemic.js (next to this
 * class) as an interactive HTML page. The script file is merged into the page
 * (embedScript): one HTML file in the home folder. Edit Epidemic.js in an editor
 * of Bio7 and run this class again to export the changed model.
 */
public class EpidemicScriptHtml {

	public static void main(String[] args) throws Exception {
		String workspace = FileRoot.getFileRoot();
		File script = new File(workspace, "Bio7_Java_Examples/src/html/Epidemic.js");
		if (!script.exists()) {
			System.err.println("The script file was not found: " + script);
			return;
		}
		String file = System.getProperty("user.home") + "/Epidemic.html";
		new HtmlSimulation("Epidemic (SIR)")
				.description("Susceptible (blue), infected (red) and recovered (green) individuals on a grid. The model is the script file " + script.getName()
						+ ".")
				.view(HtmlSimulation.View.GRID_2D)
				.heading("Population")
				.number("size", "Grid size (cells)", 150)
				.slider("density", "Density of the individuals", 0.2, 1, 0.01, 0.9)
				.slider("vaccination", "Vaccinated", 0, 0.9, 0.01, 0)
				.restartOn("size", "density", "vaccination")
				.heading("Disease")
				.slider("infection", "Infection probability (per infected neighbor)", 0, 1, 0.01, 0.25)
				.slider("recovery", "Days until the recovery", 1, 30, 1, 8)
				.slider("immunity", "Days of the immunity (0 = permanent)", 0, 200, 1, 0)
				.checkbox("moore", "Eight neighbors (Moore)", false)
				.button("Infect the center", "infectCenter")
				.chart("Susceptible, infected and recovered")
				.speed(20)
				.scriptFile(script.getPath())
				/* One HTML file (the script file is merged into the page) */
				.embedScript(true)
				.export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
