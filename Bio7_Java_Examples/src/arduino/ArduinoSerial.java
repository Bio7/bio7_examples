/*MIT License

Copyright (c) 2022 xanthium-enterprises

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/*
Adapted Serial example from the https://github.com/xanthium-enterprises Github website using JavaFX panel for a simple GUI:
Adapted read and write example from: https://github.com/xanthium-enterprises/Java-Serial-Port-Communication-Arduino-ATmega328P subsite
Please install the Arduino IDE for the driver installation of the ports!
In addition select the right port of your OS if necessary.
Serial library used: https://github.com/Fazecast/jSerialComm
For the execution Eclipse jobs were added to get or send values to avoid a blocking GUI!
Please close the serial port in the GUI (or simply close the view which also closes the port) before you recompile the class!
Upload the Arduino sketch first and probably close the Arduino IDE if it is blocking the serial output.
*
*/

package arduino;

import java.io.UnsupportedEncodingException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.collection.CustomView;
import com.fazecast.jSerialComm.SerialPort;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;

public class ArduinoSerial extends com.eco.bio7.compile.Model {
	public SerialPortReception seri;
	public SerialPort MySerialPort;
	public boolean isOpen;
	protected boolean isClosed;

	public ArduinoSerial() {

		CustomView view = new CustomView();
		view.setFxmlCanvas("Serial", FileRoot.getCurrentCompileDir() + "/arduino/Guil.fxml", this);

	}

	// Handler for Button[Button[id=null, styleClass=button]] onAction
	@FXML
	public void startIt(ActionEvent event) {
		if(MySerialPort!=null)
		if (MySerialPort.isOpen()) {
			System.out.println(MySerialPort.getSystemPortName() + " is already Open! ");
			return;
		}
		
		int BaudRate = 9600;
		int DataBits = 8;
		int StopBits = SerialPort.ONE_STOP_BIT;
		int Parity = SerialPort.NO_PARITY;

		SerialPort[] AvailablePorts = SerialPort.getCommPorts();

		System.out.println("\n\n SerialPort Data Reception");

		// use the for loop to print the available serial ports
		System.out.print("\n\n Available Ports ");
		for (int i = 0; i < AvailablePorts.length; i++) {
			System.out.println(i + " - " + AvailablePorts[i].getSystemPortName() + " -> "
					+ AvailablePorts[i].getDescriptivePortName());
		}

		// Open the right port for Arduino (select a port from the available!)
		MySerialPort = AvailablePorts[4];

		// Set Serial port Parameters
		MySerialPort.setComPortParameters(BaudRate, DataBits, StopBits, Parity);// Sets all serial port parameters at
																				// one time

		MySerialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0); // Set Read Time outs
		// .setComPortTimeouts(TIMEOUT_Mode, READ_TIMEOUT_milliSec,
		// WRITE_TIMEOUT_milliSec);

		MySerialPort.openPort(); // open the port

		if (MySerialPort.isOpen())
			System.out.println("\n" + MySerialPort.getSystemPortName() + " is Open ");
		else
			System.out.println(" Port not open ");

		// Display the Serial Port parameters
		System.out.println("\n Selected Port               = " + MySerialPort.getSystemPortName());
		System.out.println(" Selected Baud rate          = " + MySerialPort.getBaudRate());
		System.out.println(" Selected Number of DataBits = " + MySerialPort.getNumDataBits());
		System.out.println(" Selected Number of StopBits = " + MySerialPort.getNumStopBits());
		System.out.println(" Selected Parity             = " + MySerialPort.getParity());
		System.out.println(" Selected Read Time Out      = " + MySerialPort.getReadTimeout() + "mS");

		MySerialPort.flushIOBuffers();

	}

	@FXML
	public void receiveIt(ActionEvent event) {
		isClosed = true;
		Job job = new Job("Serial Port Reading") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				monitor.beginTask("Started port reading ...", IProgressMonitor.UNKNOWN);
				/* We create a feature stack. R connection not necessary! */

				try {
					while (isClosed) {
						/*Message + linebreak = 32 bytes!*/
						byte[] readBuffer = new byte[32];
						int numRead = MySerialPort.readBytes(readBuffer, readBuffer.length);
						System.out.print("Read " + numRead + " bytes -");
						// System.out.println(readBuffer);
						String S = null;
						try {
							S = new String(readBuffer, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} // convert bytes to String
						System.out.println("Received -> " + S);

					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					System.out.println("Receive Job finished!");
				} else {

				}
			}
		});

		job.schedule();

		
	}

	@FXML
	public void sendIt(ActionEvent event) {
		// Thread.sleep(2000); //Delay introduced because when the SerialPort is opened
		// ,Arduino gets resetted
		isClosed = true;
		Job job = new Job("Serial Port Writing") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				monitor.beginTask("Started port writing ...", IProgressMonitor.UNKNOWN);
				/* We create a feature stack. R connection not necessary! */

				// Time for the code in Arduino to rerun after Reset

				try {
					byte[] WriteByte = new byte[1];
					WriteByte[0] = 65; // send A

					int bytesTxed = 0;

					bytesTxed = MySerialPort.writeBytes(WriteByte, 1);

					System.out.print(" Bytes Transmitted -> " + bytesTxed);

				} catch (Exception e) {
					e.printStackTrace();
				}

				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					System.out.println("Send Job finished!");
				} else {

				}
			}
		});

		job.schedule();

	}
	
	@FXML
    public void stopActions(ActionEvent event) {
		isClosed = false;
    }

	// Handler for Button[Button[id=null, styleClass=button]] onAction
	@FXML
	public void stopit(ActionEvent event) {
		isClosed = false;
		System.out.println("Closing!");
		MySerialPort.closePort();
	}

	/*
	 * Close method (since Bio7 2.1) which will be called if a custom view is
	 * closed!
	 */

	@Override
	public void close() {		
		isClosed = false;
		if(MySerialPort!=null) {
		System.out.println("Closing Port!");
		MySerialPort.closePort();
		}
	}

}