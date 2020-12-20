package modelling.cellularautomata;

import com.eco.bio7.discrete.Field;

/*
 Please set a cell (or Plant) in the top-middle of the field to state 1. 
 Then run the setup!
 */
public class OneDimensional extends com.eco.bio7.compile.Model {
	
	public void setup() {
		mod2();
	}

	static void mod2() {

		for (int i = 0; i < Field.getHeight() - 1; i++) {
			for (int u = 0; u < Field.getWidth(); u++) {

				int modu = ((u + 1 + Field.getWidth()) % (Field.getWidth()));
				int modnu = ((u - 1 + Field.getWidth()) % (Field.getWidth()));

				if ((Field.getState(modnu, i) + Field.getState(modu, i)) % 2 == 0) {

					Field.setState(u, i + 1, 1);

				} else {
					Field.setState(u, i + 1, 0);

				}

			}
		}
	}
}