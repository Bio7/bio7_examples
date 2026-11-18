package html;

import java.io.File;


import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Ants which forage for food with pheromone trails as an HTML simulation with
 * the JavaScript model in a separate file (Ants.js next to this class). The
 * script file is merged into the page (embedScript): one HTML file in the home
 * folder. Edit Ants.js in an editor of Bio7 and run this class again to export
 * the changed model.
 */
public class AntsScriptHtml {

	public static void main(String[] args) throws Exception {
		String workspace = FileRoot.getFileRoot();
		File script = new File(workspace, "Bio7_Java_Examples/src/html/Ants.js");
		if (!script.exists()) {
			System.err.println("The script file was not found: " + script);
			return;
		}
		String file = System.getProperty("user.home") + "/Ants.html";
		new HtmlSimulation("Foraging Ants")
				.description("Ants search food, carry it to the nest (center) and lay pheromone trails which other ants follow. "
						+ "Red ants carry food. The model is the script file " + script.getName() + ".")
				.view(HtmlSimulation.View.GRID_2D)
				.heading("Colony")
				.number("size", "Grid size (cells)", 120)
				.number("ants", "Number of ants", 300)
				.slider("sources", "Food sources", 1, 8, 1, 3)
				.restartOn("size", "ants", "sources")
				.heading("Pheromone")
				.slider("deposit", "Deposit per step", 0, 1, 0.05, 0.5)
				.slider("evaporation", "Evaporation", 0, 0.1, 0.002, 0.01)
				.slider("diffusion", "Diffusion", 0, 1, 0.05, 0.3)
				.heading("Behavior")
				.slider("follow", "Follow the trail (probability)", 0, 1, 0.05, 0.9)
				.slider("randomness", "Random turns", 0, 2, 0.05, 0.6)
				.button("Add food", "addFood")
				.button("Clear the trails", "clearTrails")
				.chart("Food")
				.colorMap(new int[] { 0xF7F4EA, 0xC7E9C0, 0x41AB5D, 0x00441B, 0xFDB863 })
				.speed(30)
				.scriptFile(script.getPath())
				/* One HTML file (the script file is merged into the page) */
				.embedScript(true)
				.export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
