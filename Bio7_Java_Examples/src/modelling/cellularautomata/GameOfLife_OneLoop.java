package modelling.cellularautomata;

import static com.eco.bio7.discrete.Field.*;

/*
 The Game of Life created with one loop!
 For the game of life soil=dead;select a plant as state alive!
 */
public class GameOfLife_OneLoop extends com.eco.bio7.compile.Model {
	
	public int n = 1;// The neighbourhood value!

	public void run() {

		int i = 0;
		int u = 0;
		int i2 = 0;
		int u2 = 0;

		for (int z = 0; z < getHeight() * getWidth(); z++) {

			if (u > (getWidth() - 1)) {
				i++;
				u = 0;
			}
			// Special method to sum up the neighbourhood without the center
			// cell.
			int sum = torusMooreSum(n, u, i);

			if (sum == 2 && getState(u, i) == 1) {
				setTempState(u, i, 1);
			} else if (sum == 3 && getState(u, i) == 0) {
				setTempState(u, i, 1);
			} else if (sum == 3 && getState(u, i) == 1) {
				setTempState(u, i, 1);

			} else {
				setTempState(u, i, 0);
			}
			if (u < getWidth()) {
				u++;
			}
		}

		// Now copy the values from the temp array for the next timestep.

		for (int z = 0; z < getHeight() * getWidth(); z++) {

			if (u2 > (getWidth() - 1)) {
				i2++;
				u2 = 0;
			}

			setState(u2, i2, getTempState(u2, i2));
			if (u2 < getWidth()) {
				u2++;
			}
		}

	}
}