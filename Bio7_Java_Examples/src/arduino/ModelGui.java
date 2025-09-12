package arduino;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class ModelGui extends Composite {

	public ModelGui(Composite parent, ArduinoSerial arduinoSerial, int style) {
		super(parent, style);

		Button btnNewButton = new Button(this, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				arduinoSerial.startIt();
			}
		});
		btnNewButton.setBounds(10, 10, 111, 32);
		btnNewButton.setText("Open Port");

		Button btnStop = new Button(this, SWT.NONE);
		btnStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				arduinoSerial.stopit();
			}
		});
		btnStop.setBounds(10, 47, 111, 32);
		btnStop.setText("Close Port");

		Button btnNewButton_1 = new Button(this, SWT.NONE);
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				arduinoSerial.receiveIt();
			}
		});
		btnNewButton_1.setBounds(10, 85, 111, 32);
		btnNewButton_1.setText("Receive it");

		Button btnNewButton_2 = new Button(this, SWT.NONE);
		btnNewButton_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				arduinoSerial.stopActions();
			}
		});
		btnNewButton_2.setBounds(10, 123, 111, 32);
		btnNewButton_2.setText("Stop");

	}
}