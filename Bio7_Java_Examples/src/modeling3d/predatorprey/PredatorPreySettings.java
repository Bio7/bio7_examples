package modeling3d.predatorprey;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;

/**
 * PREDATOR-PREY SETTINGS PANEL
 * 
 * Interactive GUI for controlling all parameters of the ecosystem simulation.
 * Parameters are organized by category for easy access.
 * Real-time updates to the model as sliders are adjusted.
 * 
 * Uses ScrolledComposite to handle many parameters with proper scrolling.
 * Fonts are cached and properly disposed.
 */

public class PredatorPreySettings extends Composite {

	// Cached fonts to avoid resource leaks
	private Font boldFont14;
	private Font regularFont12;

	public PredatorPreySettings(Composite parent, final PredatorPrey3D model) {
		super(parent, SWT.NONE);
		
		// Set up main layout
		this.setLayout(new FillLayout());
		
		// Create fonts (cached for reuse)
		boldFont14 = new Font(this.getDisplay(), "Arial", 14, SWT.BOLD);
		regularFont12 = new Font(this.getDisplay(), "Arial", 12, SWT.NORMAL);
		
		// Add dispose listener to clean up resources
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (boldFont14 != null && !boldFont14.isDisposed())
					boldFont14.dispose();
				if (regularFont12 != null && !regularFont12.isDisposed())
					regularFont12.dispose();
			}
		});
		
		// Create scrolled composite for all content
		ScrolledComposite scrolled = new ScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		// Create main content composite
		Composite content = new Composite(scrolled, SWT.NONE);
		content.setLayout(new GridLayout(3, false));
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// Set scrolled composite content
		scrolled.setContent(content);
		
		// Create section labels and sliders
		createPreySection(content, model);
		addSpacer(content);
		createPredatorSection(content, model);
		addSpacer(content);
		createEnvironmentSection(content, model);
		
		// Calculate size and set min size for scrollbar
		content.pack();
		scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private void createPreySection(Composite parent, final PredatorPrey3D model) {
		// Section header
		Label header = new Label(parent, SWT.NONE);
		header.setText("PREY (HERBIVORES)");
		header.setFont(boldFont14);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.verticalIndent = 10;
		header.setLayoutData(gd);

		// Prey Move Cost: 0.1 to 1.0 (display: 0.01 to 1.00)
		addSlider(parent, "Prey Move Cost", 10, 100, (int) (model.PREY_MOVE_COST * 100), 100.0, 
			val -> model.PREY_MOVE_COST = val);
		
		// Prey Reproduce Threshold: 50 to 300 (display: actual value)
		addSlider(parent, "Reproduce Threshold", 50, 300, (int) model.PREY_REPRODUCE_THRESHOLD, 1.0,
			val -> model.PREY_REPRODUCE_THRESHOLD = val);
		
		// Prey Reproduce Cost: 30 to 150 (display: actual value)
		addSlider(parent, "Reproduce Cost", 30, 150, (int) model.PREY_REPRODUCE_COST, 1.0,
			val -> model.PREY_REPRODUCE_COST = val);
		
		// Grass Energy: 10 to 100 (display: actual value)
		addSlider(parent, "Grass Energy", 10, 100, (int) model.GRASS_ENERGY, 1.0,
			val -> model.GRASS_ENERGY = val);
		
		// Prey Sense Range: 5 to 40 (display: actual value)
		addSlider(parent, "Prey Sense Range", 5, 40, (int) model.PREY_SENSE_RANGE, 1.0,
			val -> model.PREY_SENSE_RANGE = val);
		
		// Prey Move Speed: 0.5 to 3.0 (display: 0.05 to 3.00)
		addSlider(parent, "Prey Move Speed", 5, 30, (int) (model.PREDATOR_MOVE_SPEED * 10), 10.0,
			val -> model.PREDATOR_MOVE_SPEED = val);
	}

	private void createPredatorSection(Composite parent, final PredatorPrey3D model) {
		// Section header
		Label header = new Label(parent, SWT.NONE);
		header.setText("PREDATORS (CARNIVORES)");
		header.setFont(boldFont14);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.verticalIndent = 10;
		header.setLayoutData(gd);

		// Predator Move Cost: 0.5 to 2.0 (display: 0.05 to 2.00)
		addSlider(parent, "Predator Move Cost", 5, 200, (int) (model.PREDATOR_MOVE_COST * 100), 100.0,
			val -> model.PREDATOR_MOVE_COST = val);
		
		// Predator Reproduce Threshold: 80 to 300 (display: actual value)
		addSlider(parent, "Reproduce Threshold", 80, 300, (int) model.PREDATOR_REPRODUCE_THRESHOLD, 1.0,
			val -> model.PREDATOR_REPRODUCE_THRESHOLD = val);
		
		// Predator Reproduce Cost: 40 to 200 (display: actual value)
		addSlider(parent, "Reproduce Cost", 40, 200, (int) model.PREDATOR_REPRODUCE_COST, 1.0,
			val -> model.PREDATOR_REPRODUCE_COST = val);
		
		// Predator Gain from Prey: 30 to 150 (display: actual value)
		addSlider(parent, "Gain from Prey", 30, 150, (int) model.PREDATOR_GAIN_FROM_PREY, 1.0,
			val -> model.PREDATOR_GAIN_FROM_PREY = val);
		
		// Predator Hunt Sense Range: 5 to 50 (display: actual value)
		addSlider(parent, "Hunt Sense Range", 5, 50, (int) model.PREDATOR_SENSE_RANGE, 1.0,
			val -> model.PREDATOR_SENSE_RANGE = val);
		
		// Predator Hunt Speed: 0.5 to 3.0 (display: 0.05 to 3.00)
		addSlider(parent, "Hunt Speed", 5, 30, (int) (model.PREDATOR_HUNT_SPEED * 10), 10.0,
			val -> model.PREDATOR_HUNT_SPEED = val);
	}

	private void createEnvironmentSection(Composite parent, final PredatorPrey3D model) {
		// Section header
		Label header = new Label(parent, SWT.NONE);
		header.setText("ENVIRONMENT & LIMITS");
		header.setFont(boldFont14);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.verticalIndent = 10;
		header.setLayoutData(gd);

		// Grass Regen Rate: 0.05 to 0.50 (display: 0.05 to 0.50)
		addSlider(parent, "Grass Regen Rate (%)", 5, 50, (int) (model.GRASS_REGEN_RATE * 100), 100.0,
			val -> model.GRASS_REGEN_RATE = val);
		
		// Max Prey: 200 to 2000 (step by 50)
		addSlider(parent, "Max Prey", 4, 40, model.MAX_PREY / 50, 50.0,
			val -> model.MAX_PREY = val.intValue());
		
		// Max Predators: 20 to 400 (step by 5)
		addSlider(parent, "Max Predators", 4, 80, model.MAX_PREDATORS / 5, 5.0,
			val -> model.MAX_PREDATORS = val.intValue());
		
		// Max Grass: 1000 to 10000 (step by 500)
		addSlider(parent, "Max Grass", 2, 20, model.MAX_GRASS / 500, 500.0,
			val -> model.MAX_GRASS = val.intValue());
	}

	private void addSpacer(Composite parent) {
		Label spacer = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.heightHint = 10;
		spacer.setLayoutData(gd);
	}

	/**
	 * Helper method to create a parameter row with a slider and real-time listener
	 * 
	 * @param parent - Parent composite
	 * @param labelText - Label text for the parameter
	 * @param min - Minimum slider value
	 * @param max - Maximum slider value
	 * @param current - Current slider value (pre-scaled)
	 * @param divisor - Divisor to convert slider value to actual parameter value
	 * @param update - Consumer function to update the model parameter
	 */
	private void addSlider(Composite parent, String labelText, int min, int max, int current, 
			final double divisor, final java.util.function.Consumer<Double> update) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		label.setFont(regularFont12);
		GridData labelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		labelData.widthHint = 180;
		label.setLayoutData(labelData);

		// The Slider (Scale)
		final Scale scale = new Scale(parent, SWT.HORIZONTAL);
		scale.setMinimum(min);
		scale.setMaximum(max);
		scale.setSelection(Math.max(min, Math.min(max, current))); // Clamp value
		GridData scaleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		scaleData.minimumWidth = 150;
		scale.setLayoutData(scaleData);

		// Value Display
		final Label valDisplay = new Label(parent, SWT.NONE);
		valDisplay.setText(String.format("%.2f", current / divisor));
		valDisplay.setFont(regularFont12);
		GridData textData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		textData.widthHint = 80;
		valDisplay.setLayoutData(textData);

		// Selection Listener for real-time model updates
		scale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				double calculatedValue = scale.getSelection() / divisor;
				// Update the model variable via Consumer
				update.accept(calculatedValue);
				// Update the UI label display
				valDisplay.setText(String.format("%.2f", calculatedValue));
			}
		});
	}
}