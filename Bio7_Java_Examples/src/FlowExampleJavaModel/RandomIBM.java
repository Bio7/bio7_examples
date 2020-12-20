package FlowExampleJavaModel;

import com.eco.bio7.database.Bio7State;
import com.eco.bio7.discrete.Field;




public class RandomIBM extends com.eco.bio7.compile.Model {

public int zuff;// The random number.

public void run() {
	for (int y = 0; y < Field.getHeight(); y++) {

		for (int x = 0; x < Field.getWidth(); x++) {
			
			zuff = (int) (Math.random() * (Bio7State.getStateSize()));
			

			if (zuff >=0) {
			/* Zero is the value of the soil !*/
			
                /*We fill the state array !*/
				Field.setState(x, y, zuff); 
				Field.setTempState(x, y, zuff);
				
			}	

		}
	}
}
}