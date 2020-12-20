/*
A Groovy JavaFX example shows a JavaFX chart embedded in a custom view!
*/

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;


class Model {
	
@FXML  BubbleChart<?, ?> bubble; // Value injected by FXMLLoader
@FXML  PieChart pie; // Value injected by FXMLLoader

public  Model() {
	 Model m=this;
	CustomView view = new CustomView();
	//Composite parent = view.getComposite("custompanel");
	Display dis = view.getDisplay();

	dis.asyncExec(new Runnable() {
		public void run() {

			view.setFxmlCanvas("Title", FileRoot.getFileRoot()+ "/Bio7_Script_Examples/Groovy/JavaFX/example.fxml", m);
			//or as a menu: view.setFxmlCanvas("Title", FileRoot.getRShellScriptLocation()+ "/JavaFX2/example.fxml", m);
			
		}
	});

}

@FXML protected void buttonPressed(ActionEvent event) {
	         
       ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList(
                    new PieChart.Data("January", 100),
                    new PieChart.Data("February", 200),
                    new PieChart.Data("March", 50),
                    new PieChart.Data("April", 75),
                    new PieChart.Data("May", 110),
                    new PieChart.Data("June", 300),
                    new PieChart.Data("July", 111),
                    new PieChart.Data("August", 30),
                    new PieChart.Data("September", 75),
                    new PieChart.Data("October", 55),
                    new PieChart.Data("November", 225),
                    new PieChart.Data("December", 99));
         
                pie.setData(pieChartData);   
	}
	
}

Model m =new Model();
        