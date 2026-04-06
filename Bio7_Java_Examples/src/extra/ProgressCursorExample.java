/*A console progress bar example using ANSI color codes in the default Bio7 console!*/
package extra;

public class ProgressCursorExample {
    public static void main(String[] args) {
        int totalSteps = 20; // Increased for a better visual

        for (int step = 0; step <= totalSteps; step++) {
            int progress = (step * 100) / totalSteps;

            // Use \r at the very start to overwrite the previous line
            System.out.print("\rProgress: " + progress + "% [");
            
            // Build the bar
            for (int i = 0; i < totalSteps; i++) {
                if (i < step) {
                    System.out.print("=");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.print("]");

            // Flush the output to ensure it shows up immediately
            System.out.flush();

            try {
                Thread.sleep(200); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("\nDone!");
    }
}