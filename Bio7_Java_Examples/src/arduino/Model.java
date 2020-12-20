package arduino;

/*
Adapted Serial example from the Arduino website using JavaFX panel for a simple GUI:
Example from: http://playground.arduino.cc/Interfacing/Java
Please install the Arduino IDE for the driver installation of the ports!
In addition select the right port of your OS (see SerialTest.java).
*/
import javafx.fxml.FXML;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import com.eco.bio7.collection.*;
import javafx.event.ActionEvent;
import com.eco.bio7.batch.*;

public class Model extends com.eco.bio7.compile.Model {
	public  SerialTest serial;
	public Model main;
	
	public Model(){
		
		CustomView view = new CustomView();     
		view.setFxmlCanvas("Embedd", FileRoot.getFileRoot()+ "/Bio7_Java_Examples/src/arduino/Guil.fxml", this);
		serial = new SerialTest();
			
	}
	public void setup(){


	}
	
	

	public static void main(String[] args) {

		
	}
	
    // Handler for Button[Button[id=null, styleClass=button]] onAction
   @FXML public void startIt(ActionEvent event) {
        serial.initialize();
		
    }
	

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML public void stopit(ActionEvent event) {
       serial.close();
    }
    /*Close method (since Bio7 2.1) which will be called if a custom view is closed!*/
    public void close(){
    	serial.close();
    }
	
	
}