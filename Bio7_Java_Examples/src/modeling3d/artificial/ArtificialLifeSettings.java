package modeling3d.artificial;

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
 * ARTIFICIAL LIFE SETTINGS PANEL
 * 
 * Controls for creature evolution parameters:
 * - Population settings
 * - Energy parameters
 * - Mutation rates
 * - Neural network tuning
 */

public class ArtificialLifeSettings extends Composite {

	private Font boldFont14;
	private Font regularFont12;

	public ArtificialLifeSettings(Composite parent, final ArtificialLife3D model) {
		super(parent, SWT.NONE);
		
		this.setLayout(new FillLayout());
		
		boldFont14 = new Font(this.getDisplay(), "Arial", 14, SWT.BOLD);
		regularFont12 = new Font(this.getDisplay(), "Arial", 12, SWT.NORMAL);
		
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (boldFont14 != null && !boldFont14.isDisposed())
					boldFont14.dispose();
				if (regularFont12 != null && !regularFont12.isDisposed())
					regularFont12.dispose();
			}
		});
		
		ScrolledComposite scrolled = new ScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite content = new Composite(scrolled, SWT.NONE);
		content.setLayout(new GridLayout(3, false));
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		scrolled.setContent(content);
		
		createPopulationSection(content, model);
		addSpacer(content);
		createEnergySection(content, model);
		addSpacer(content);
		createEvolutionSection(content, model);
		
		content.pack();
		scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private void createPopulationSection(Composite parent, final ArtificialLife3D model) {
		Label header = new Label(parent, SWT.NONE);
		header.setText("POPULATION SETTINGS");
		header.setFont(boldFont14);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.verticalIndent = 10;
		header.setLayoutData(gd);

		addSlider(parent, "Initial Population", 10, 200, model.INITIAL_POPULATION, 1.0,
			val -> model.INITIAL_POPULATION = val.intValue());
	}

	private void createEnergySection(Composite parent, final ArtificialLife3D model) {
		Label header = new Label(parent, SWT.NONE);
		header.setText("ENERGY PARAMETERS");
		header.setFont(boldFont14);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.verticalIndent = 10;
		header.setLayoutData(gd);

		addSlider(parent, "Energy Cost Per Step", 1, 50, (int) (model.ENERGY_COST_PER_STEP * 100), 100.0,
			val -> model.ENERGY_COST_PER_STEP = val);

		addSlider(parent, "Energy From Food", 10, 200, (int) (model.ENERGY_FROM_FOOD / 5), 5.0,
			val -> model.ENERGY_FROM_FOOD = val);

		addSlider(parent, "Reproduce Threshold", 50, 300, (int) model.REPRODUCE_THRESHOLD, 1.0,
			val -> model.REPRODUCE_THRESHOLD = val);
	}

	private void createEvolutionSection(Composite parent, final ArtificialLife3D model) {
		Label header = new Label(parent, SWT.NONE);
		header.setText("EVOLUTION & MUTATION");
		header.setFont(boldFont14);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.verticalIndent = 10;
		header.setLayoutData(gd);

		addSlider(parent, "Mutation Rate (%)", 1, 50, (int) (model.MUTATION_RATE * 100), 100.0,
			val -> model.MUTATION_RATE = val);

		addSlider(parent, "Mutation Amount", 1, 100, (int) (model.MUTATION_AMOUNT * 100), 100.0,
			val -> model.MUTATION_AMOUNT = val);
	}

	private void addSpacer(Composite parent) {
		Label spacer = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.heightHint = 10;
		spacer.setLayoutData(gd);
	}

	private void addSlider(Composite parent, String labelText, int min, int max, int current, 
			final double divisor, final java.util.function.Consumer<Double> update) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		label.setFont(regularFont12);
		GridData labelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		labelData.widthHint = 180;
		label.setLayoutData(labelData);

		final Scale scale = new Scale(parent, SWT.HORIZONTAL);
		scale.setMinimum(min);
		scale.setMaximum(max);
		scale.setSelection(Math.max(min, Math.min(max, current)));
		GridData scaleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		scaleData.minimumWidth = 150;
		scale.setLayoutData(scaleData);

		final Label valDisplay = new Label(parent, SWT.NONE);
		valDisplay.setText(String.format("%.2f", current / divisor));
		valDisplay.setFont(regularFont12);
		GridData textData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		textData.widthHint = 80;
		valDisplay.setLayoutData(textData);

		scale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				double calculatedValue = scale.getSelection() / divisor;
				update.accept(calculatedValue);
				valDisplay.setText(String.format("%.2f", calculatedValue));
			}
		});
	}
}