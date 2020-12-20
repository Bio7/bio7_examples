package gui;

/*
 * GUI from Opal widgets: https://code.google.com/a/eclipselabs.org/p/opal/
 * Source from Opal examples: https://code.google.com/a/eclipselabs.org/p/opal/wiki/Calculator
 * 
 * */
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.mihalis.opal.calculator.Calculator;

import com.eco.bio7.collection.CustomView;

public class Opal_Calc extends com.eco.bio7.compile.Model {

	public Opal_Calc() {
		CustomView view = new CustomView();

		Display dis = CustomView.getDisplay();

		dis.syncExec(new Runnable() {
			public void run() {

				Composite parent = view.getComposite("custompanel");

				Calculator calc = new Calculator(parent, SWT.NONE);

				calc.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(final ModifyEvent e) {
						System.out.println("New value is " + calc.getValue());
					}
				});
				/* Here we have to call the layout method! */
				parent.layout();
			}
		});

	}

}