package modeling3d.swarm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class Settings extends Composite {

	public Settings(Composite parent, final BoidSphereModel model) {
		super(parent, SWT.NONE);
		// 3-column layout: Label | Slider | Value Display
		this.setLayout(new GridLayout(3, false));

		// --- Parameter Rows ---
		// Max Speed: Range 0.1 to 20.0
		addSlider("Max Speed", 1, 200, (int) (model.maxSpeed * 10), 10.0, val -> model.maxSpeed = val);

		// Max Force: Range 0.01 to 1.0 (Higher precision divisor)
		addSlider("Max Force", 1, 100, (int) (model.maxForce * 100), 100.0, val -> model.maxForce = val);

		// Weights: Range 0.0 to 10.0
		addSlider("Separation", 0, 100, (int) (model.separationWeight * 10), 10.0, val -> model.separationWeight = val);
		addSlider("Alignment", 0, 100, (int) (model.alignmentWeight * 10), 10.0, val -> model.alignmentWeight = val);
		addSlider("Cohesion", 0, 100, (int) (model.cohesionWeight * 10), 10.0, val -> model.cohesionWeight = val);

		// Radius: Range 1 to 50 (Cast to float for the model)
		addSlider("Radius", 1, 50, (int) model.radius, 1.0, val -> model.radius = val.floatValue());

		// Cam Dist: Range 5 to 500
		addSlider("Cam Dist", 5, 500, (int) model.camDist, 1.0, val -> model.camDist = val);

		addSlider("Floor Size", 1, 300, model.FLOOR_LEN, 1, val -> model.FLOOR_LEN = val.intValue());
	}

	/**
	 * Helper method to create a parameter row with a slider and real-time listener
	 */
	private void addSlider(String labelText, int min, int max, int current, final double divisor,
			final java.util.function.Consumer<Double> update) {

		new Label(this, SWT.NONE).setText(labelText);

		// The Slider (Scale)
		final Scale scale = new Scale(this, SWT.HORIZONTAL);
		scale.setMinimum(min);
		scale.setMaximum(max);
		scale.setSelection(current);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Value Display
		final Label valDisplay = new Label(this, SWT.NONE);
		valDisplay.setText(String.valueOf(current / divisor));
		GridData textData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		textData.widthHint = 50; // Slightly wider for decimal numbers
		valDisplay.setLayoutData(textData);

		// Selection Listener for real-time model updates
		scale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				double calculatedValue = scale.getSelection() / divisor;
				// Update the model variable via Consumer
				update.accept(calculatedValue);
				// Update the UI label display
				valDisplay.setText(String.valueOf(calculatedValue));
			}
		});
	}
}
