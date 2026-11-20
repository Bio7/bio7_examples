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
		// The number of boids: 10 to 1000 (added or removed at the next frame)
		addSlider("Boids", 10, 1000, model.boidCount, 1.0, val -> model.boidCount = val.intValue());
		// Max Speed: Range 0.1 to 20.0
		addSlider("Max Speed", 1, 200, (int) (model.maxSpeed * 10), 10.0, val -> model.maxSpeed = val);

		// Max Force: Range 0.01 to 1.0 (Higher precision divisor)
		addSlider("Max Force", 1, 100, (int) (model.maxForce * 100), 100.0, val -> model.maxForce = val);

		// Weights: Range 0.0 to 10.0
		addSlider("Separation", 0, 100, (int) (model.separationWeight * 10), 10.0, val -> model.separationWeight = val);
		addSlider("Alignment", 0, 100, (int) (model.alignmentWeight * 10), 10.0, val -> model.alignmentWeight = val);
		addSlider("Cohesion", 0, 100, (int) (model.cohesionWeight * 10), 10.0, val -> model.cohesionWeight = val);

		// The size of the gliders: range 1 to 50 (cast to float for the model)
		addSlider("Size", 1, 50, (int) model.radius, 1.0, val -> model.radius = val.floatValue());

		// Cam Dist: Range 5 to 500
		addSlider("Cam Dist", 5, 500, (int) model.camDist, 1.0, val -> model.camDist = val);

		// The size of the floor (in 100 units): also the flight space, the box and the axes of the view
		addSlider("Floor Size", 2, 300, model.FLOOR_LEN, 1, val -> model.setFloorSize(val.intValue()));

		// Obstacles: on/off, the number, the placement (random or fixed in the centre)
		final Button chkObstacles = new Button(this, SWT.CHECK);
		chkObstacles.setText("Obstacles (pillars)");
		chkObstacles.setSelection(model.obstaclesEnabled);
		chkObstacles.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		chkObstacles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.obstaclesEnabled = chkObstacles.getSelection();
				model.requestObstacles();
			}
		});
		addSlider("Obstacles", 1, 50, model.obstacleCount, 1.0, val -> {
			model.obstacleCount = val.intValue();
			model.requestObstacles();
		});
		new Label(this, SWT.NONE).setText("Placement");
		final Combo comboPlacement = new Combo(this, SWT.READ_ONLY);
		comboPlacement.setItems(BoidSphereModel.PLACEMENTS);
		comboPlacement.setText(model.obstaclePlacement);
		comboPlacement.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboPlacement.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.obstaclePlacement = comboPlacement.getText();
				model.requestObstacles();
			}
		});
		final Button btnNewPositions = new Button(this, SWT.PUSH);
		btnNewPositions.setText("New positions");
		btnNewPositions.setToolTipText("New random positions of the obstacles (placement Random)");
		btnNewPositions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.requestObstacles();
			}
		});

		// The angle of the global view (a mouse rotation is reset)
		new Label(this, SWT.NONE).setText("Global view");
		final Combo comboView = new Combo(this, SWT.READ_ONLY);
		comboView.setItems(BoidSphereModel.GLOBAL_VIEWS);
		comboView.setText(model.globalView);
		comboView.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		comboView.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.globalView = comboView.getText();
				model.applyGlobalView();
			}
		});
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
