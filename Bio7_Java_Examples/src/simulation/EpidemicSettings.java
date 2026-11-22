package simulation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * The settings panel of the Epidemic model, designed with the WindowBuilder
 * (open the class with the WindowBuilder editor to change it). The widgets are
 * bound to the model by the names of the fields (see
 * SimulationSettings.bind): a Scale, Slider, Spinner, Text, check Button or
 * Combo with the name of a Param field, a Label with the name and "Value"
 * shows the value, a push Button with the name of an Action method calls it
 * (setup and export: the setup and the HTML export).
 */
public class EpidemicSettings extends Composite {

	private Scale gridSize;
	private Label gridSizeValue;
	private Button torus;
	private Combo neighborhood;
	private Scale infection;
	private Label infectionValue;
	private Spinner recoveryDays;
	private Spinner immunityDays;
	private Text initialInfected;
	private Combo colors;
	private Slider vaccineShare;
	private Label vaccineShareValue;
	private Button infectRandom;
	private Button vaccinate;
	private Button setup;
	private Button export;

	public EpidemicSettings(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Group grpWorld = new Group(this, SWT.NONE);
		grpWorld.setText("World");
		grpWorld.setLayout(new GridLayout(2, false));
		grpWorld.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblGridSize = new Label(grpWorld, SWT.NONE);
		lblGridSize.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblGridSize.setText("Grid size (cells)");

		gridSize = new Scale(grpWorld, SWT.NONE);
		gridSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		gridSizeValue = new Label(grpWorld, SWT.RIGHT);
		GridData gdGridSizeValue = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gdGridSizeValue.widthHint = 50;
		gridSizeValue.setLayoutData(gdGridSizeValue);

		torus = new Button(grpWorld, SWT.CHECK);
		torus.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		torus.setText("Torus world (wrap at the borders)");

		Label lblNeighborhood = new Label(grpWorld, SWT.NONE);
		lblNeighborhood.setText("Neighborhood");

		neighborhood = new Combo(grpWorld, SWT.READ_ONLY);
		neighborhood.setItems(new String[] { "Moore (8 cells)", "von Neumann (4 cells)" });
		neighborhood.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Group grpDisease = new Group(this, SWT.NONE);
		grpDisease.setText("Disease");
		grpDisease.setLayout(new GridLayout(2, false));
		grpDisease.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblInfection = new Label(grpDisease, SWT.NONE);
		lblInfection.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblInfection.setText("Infection probability (per infected neighbor)");

		infection = new Scale(grpDisease, SWT.NONE);
		infection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		infectionValue = new Label(grpDisease, SWT.RIGHT);
		GridData gdInfectionValue = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gdInfectionValue.widthHint = 50;
		infectionValue.setLayoutData(gdInfectionValue);

		Label lblRecovery = new Label(grpDisease, SWT.NONE);
		lblRecovery.setText("Days until the recovery");

		recoveryDays = new Spinner(grpDisease, SWT.BORDER);
		recoveryDays.setMinimum(1);
		recoveryDays.setMaximum(60);

		Label lblImmunity = new Label(grpDisease, SWT.NONE);
		lblImmunity.setText("Days of immunity (0: for ever)");

		immunityDays = new Spinner(grpDisease, SWT.BORDER);
		immunityDays.setMaximum(365);

		Group grpStart = new Group(this, SWT.NONE);
		grpStart.setText("Start");
		grpStart.setLayout(new GridLayout(2, false));
		grpStart.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblInitial = new Label(grpStart, SWT.NONE);
		lblInitial.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblInitial.setText("Infected cells at the start (x,y; x,y)");

		initialInfected = new Text(grpStart, SWT.BORDER);
		initialInfected.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label lblColors = new Label(grpStart, SWT.NONE);
		lblColors.setText("Colors");

		colors = new Combo(grpStart, SWT.READ_ONLY);
		colors.setItems(new String[] { "Classic", "Pastel" });
		colors.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Group grpInterventions = new Group(this, SWT.NONE);
		grpInterventions.setText("Interventions");
		grpInterventions.setLayout(new GridLayout(2, false));
		grpInterventions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblShare = new Label(grpInterventions, SWT.NONE);
		lblShare.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblShare.setText("Share of a vaccination");

		vaccineShare = new Slider(grpInterventions, SWT.NONE);
		vaccineShare.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		vaccineShareValue = new Label(grpInterventions, SWT.RIGHT);
		GridData gdVaccineShareValue = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gdVaccineShareValue.widthHint = 50;
		vaccineShareValue.setLayoutData(gdVaccineShareValue);

		infectRandom = new Button(grpInterventions, SWT.NONE);
		infectRandom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		infectRandom.setText("Infect a random cell");

		vaccinate = new Button(grpInterventions, SWT.NONE);
		vaccinate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		vaccinate.setText("Vaccinate the susceptible cells (share)");

		Composite buttons = new Composite(this, SWT.NONE);
		buttons.setLayout(new GridLayout(2, true));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		setup = new Button(buttons, SWT.NONE);
		setup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		setup.setText("Setup");

		export = new Button(buttons, SWT.NONE);
		export.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		export.setText("Export HTML...");
	}

	@Override
	protected void checkSubclass() {
		/* Allows the subclass of a SWT component */
	}
}
