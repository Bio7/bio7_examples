package html;

import java.io.File;


import com.eco.bio7.spatial.HtmlSimulation;

/**
 * The Game of Life as an HTML simulation with the JavaScript model in a
 * separate file (GameOfLife.js next to this class): the model can be edited in
 * an editor of Bio7 (e.g., the generic editor) and the page loads the file. The
 * export writes the page and copies the script file next to it (home folder).
 */
public class GameOfLifeScriptHtml {

	public static void main(String[] args) throws Exception {
		/* The script file in the workspace (else a template in the home folder) */
		String workspace = FileRoot.getFileRoot();
		File script = new File(workspace, "Bio7_Java_Examples/src/html/GameOfLife.js");
		if (!script.exists()) {
			script = new File(System.getProperty("user.home"), "GameOfLife.js");
			HtmlSimulation.writeScriptTemplate(script.getPath());
			System.out.println("Created a template of the script: " + script);
		}
		String file = System.getProperty("user.home") + "/GameOfLifeScript.html";
		new HtmlSimulation("Game of Life (script file)")
				.description("The model is loaded from " + script.getName() + " (next to this page).")
				.view(HtmlSimulation.View.GRID_2D)
				.number("size", "Grid size (cells)", 120)
				.slider("density", "Start density", 0, 1, 0.01, 0.3)
				.checkbox("wrap", "Torus world", true)
				.choice("colors", "Colors", new String[] { "Light", "Dark" }, "Light")
				.text("rule", "Rule", "B3/S23")
				.restartOn("size", "density", "colors")
				.button("Randomize", "randomize")
				.button("Add a glider", "glider")
				.chart("Population")
				.scriptFile(script.getPath())
				.export(file);
		System.out.println("Exported: " + file + " and " + script.getName());
		HtmlSimulation.open(file);
	}
}
