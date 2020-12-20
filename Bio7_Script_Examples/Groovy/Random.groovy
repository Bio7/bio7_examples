/*
A simple groovy example to show how to produce and set 
random cells without to set any individual plant-data
information from the database. 
*/
import static com.eco.bio7.discrete.Field.*;

def rand; // A random number variable!
def count=0;
while (count<600){
	

	for (def y = 0; y < getHeight(); y++) {

		for (def x = 0; x < getWidth(); x++) {
			rand = (int) (Math.random() * 3);
			setState(x,y,rand);
			setTempState(x,y,rand);

		}
	}
	count++;
	doPaint();

}
