package modelling.random;

import static com.eco.bio7.discrete.Field.*;

/*
 This example demonstrates a simple random walk in the discrete grid!
 Please add a state (Plant) to the discrete Field!
 */

public class RandomWalk extends com.eco.bio7.compile.Model {

	public void run() {

		randomwalk();

	}

	static void randomwalk() {
		int tmpi = 0;
		int tmpu = 0;
		int zuf[] = { 1, 0, -1 };

		for (int i = 0; i < getHeight(); i++) {
			for (int u = 0; u < getWidth(); u++) {
				int zi = (int) (Math.random() * 3);
				int zu = (int) (Math.random() * 3);

				if (getState(u, i) == 1) {

					setState(u, i, 0);
					tmpi = ((i + zuf[zi] + getHeight()) % (getHeight()));
					tmpu = ((u + zuf[zu] + getWidth()) % (getWidth()));

				}
			}
		}

		setState(tmpu, tmpi, 1);
	}
}