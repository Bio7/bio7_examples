package html;

import java.io.File;


import com.eco.bio7.spatial.HtmlSimulation;

/**
 * The Gray-Scott reaction-diffusion model as an HTML simulation with the
 * JavaScript model in a separate file (ReactionDiffusion.js next to this
 * class). The page loads the script file: the export writes the page and copies
 * the script next to it (home folder). Edit the copy and reload the page, or
 * edit ReactionDiffusion.js in Bio7 and run this class again.
 */
public class ReactionDiffusionScriptHtml {

	public static void main(String[] args) throws Exception {
		String workspace = FileRoot.getFileRoot();
		File script = new File(workspace, "Bio7_Java_Examples/src/html/ReactionDiffusion.js");
		if (!script.exists()) {
			System.err.println("The script file was not found: " + script);
			return;
		}
		String file = System.getProperty("user.home") + "/ReactionDiffusion.html";
		new HtmlSimulation("Reaction-Diffusion (Gray-Scott)")
				.description("Two substances which react and diffuse (U + 2V → 3V) form spots, stripes, spirals or mazes. "
						+ "The color is the concentration of V. The model is the script file " + script.getName() + ".")
				.view(HtmlSimulation.View.GRID_2D)
				.heading("Model")
				.number("size", "Grid size (cells)", 160)
				.slider("seeds", "Start seeds", 1, 30, 1, 8)
				.restartOn("size", "seeds")
				.choice("preset", "Pattern", new String[] { "Coral", "Mitosis", "Spots", "Spirals", "Worms", "Maze", "Custom" }, "Coral")
				.slider("feed", "Feed rate F (Custom)", 0.005, 0.1, 0.0005, 0.04)
				.slider("kill", "Kill rate k (Custom)", 0.04, 0.075, 0.0005, 0.06)
				.slider("iterations", "Iterations per step", 1, 40, 1, 12)
				.button("Add a seed", "seed")
				.button("Clear", "clearV")
				.chart("Mean concentration of V")
				/* A custom color map (dark - light) */
				.colorMap(new int[] { 0x000004, 0x3B0F70, 0x8C2981, 0xDE4968, 0xFE9F6D, 0xFCFDBF })
				.speed(30)
				/* The page loads the script file (copied next to the page) */
				.scriptFile(script.getPath())
				.export(file);
		System.out.println("Exported: " + file + " and " + script.getName());
		HtmlSimulation.open(file);
	}
}
