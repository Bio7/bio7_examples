/*A more complex console progress bar example using ANSI color codes in the default Bio7 console!*/
package extra;

public class ConsoleDashboard {

	// --- Standard ANSI Escape Sequences ---
	private static final String RESET = "\u001B[0m";
	private static final String BOLD = "\u001B[1m";
	private static final String CLEAR = "\u001B[2J\u001B[H";

	// --- Compatibility Color Palette (30-37 Range) ---
	private static final String GREEN = "\u001B[32m"; // Standard Green
	private static final String CYAN = ""; // Standard Cyan
	private static final String RED = "\u001B[31m"; // Standard Red
	private static final String YELLOW = "\u001B[33m"; // Standard Yellow
	private static final String BG_DARK = "\u001B[40m"; // Black Background

	public static void main(String[] args) throws InterruptedException {
		// Clear screen (only if ANSI is supported)

		System.out.print(CLEAR);

		// 1. Draw Styled Header
		printHeader("NEURAL LINK INTERFACE", 60);

		// 2. Metrics Display
		System.out.printf("%s%-15s%s %sACTIVE%s\n", color(CYAN), "CONNECTION:", color(RESET), color(GREEN + BOLD),
				color(RESET));
		System.out.printf("%s%-15s%s %s%d ms%s\n\n", color(CYAN), "LATENCY:", color(RESET), color(YELLOW), 14,
				color(RESET));

		// 3. Simulated Task Sequence
		String[] tasks = { "Syncing Nodes", "Loading Assets", "Securing Tunnel" };

		for (String task : tasks) {
			runTask(task);
		}

		System.out.println("\n\n" + color(GREEN) + "» SYSTEM READY. WELCOME, OPERATOR." + color(RESET));
	}

	// --- Conditional color wrapper ---
	private static String color(String ansiCode) {
		return  ansiCode ;
	}

	private static void printHeader(String title, int width) {
		String padding = " ".repeat(Math.max(0, (width - title.length()) / 2));
		System.out.println(color(BG_DARK) + color(YELLOW) + color(BOLD) + "=".repeat(width));
		System.out.println(padding + title + padding);
		System.out.println("=".repeat(width) + color(RESET) + "\n");
	}

	private static void runTask(String taskName) throws InterruptedException {
		for (int i = 0; i <= 100; i += 10) {
			updateProgressBar(taskName, i);
			Thread.sleep(80);
		}
		System.out.println();
	}

	private static void updateProgressBar(String label, int percentage) {
		int barWidth = 30;
		int filled = (int) (barWidth * (percentage / 100.0));

		String filledPart = "█".repeat(filled);
		String emptyPart = "-".repeat(barWidth - filled);
		String bar = filledPart + emptyPart;

		// Build the complete line, then apply colors/reset at the end
		String line = String.format("%-20s [%s] %3d%%", label, bar, percentage);

		
			System.out.print("\r" + CYAN + line);
		
		System.out.flush();
	}

}