package modeling3d.gameoflife2d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import java.util.Map;
import java.util.TreeMap;

public class GameOfLifeSettings extends Composite {

	private Font boldFont16;
	private Font regularFont11;
	private Label speedValue;
	private Label populationValue;
	private Label gridWidthValue;
	private Label gridHeightValue;
	private Label zoomValue;
	private Table patternTable;
	private Map<String, int[][]> patterns;

	public GameOfLifeSettings(Composite parent, final GameOfLife3D model) {
		super(parent, SWT.NONE);

		this.setLayout(new FillLayout());
		this.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		boldFont16 = new Font(this.getDisplay(), "Arial", 16, SWT.BOLD);
		regularFont11 = new Font(this.getDisplay(), "Arial", 11, SWT.NORMAL);

		patterns = createPatterns();

		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (boldFont16 != null && !boldFont16.isDisposed())
					boldFont16.dispose();
				if (regularFont11 != null && !regularFont11.isDisposed())
					regularFont11.dispose();
			}
		});

		ScrolledComposite scrolled = new ScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);

		Composite content = new Composite(scrolled, SWT.NONE);
		content.setLayout(new GridLayout(1, false));
		content.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		scrolled.setContent(content);

		createSimulationSection(content, model);
		createGridSection(content, model);
		createZoomSection(content, model);
		createPatternSection(content, model);
		createButtonSection(content, model);

		content.pack();
		scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private Map<String, int[][]> createPatterns() {
		Map<String, int[][]> map = new TreeMap<>();

		map.put("001_Glider", new int[][] { { 0, 1 }, { 1, 2 }, { 2, 0 }, { 2, 1 }, { 2, 2 } });
		map.put("002_Glider_Gun",
				new int[][] { { 24, 0 }, { 22, 1 }, { 24, 1 }, { 12, 2 }, { 13, 2 }, { 20, 2 }, { 21, 2 }, { 34, 2 },
						{ 35, 2 }, { 11, 3 }, { 15, 3 }, { 20, 3 }, { 21, 3 }, { 35, 3 }, { 0, 4 }, { 1, 4 }, { 11, 4 },
						{ 16, 4 }, { 20, 4 }, { 21, 4 }, { 0, 5 }, { 1, 5 }, { 11, 5 }, { 15, 5 }, { 22, 5 }, { 24, 5 },
						{ 13, 6 }, { 23, 6 }, { 24, 6 }, { 35, 6 }, { 23, 7 }, { 24, 7 }, { 34, 7 }, { 35, 7 } });
		map.put("003_LWSS", new int[][] { { 0, 0 }, { 0, 3 }, { 1, 4 }, { 2, 0 }, { 2, 4 }, { 3, 0 }, { 3, 1 },
				{ 3, 2 }, { 3, 4 } });
		map.put("004_Two_Gliders", new int[][] { { 0, 1 }, { 1, 2 }, { 2, 0 }, { 2, 1 }, { 2, 2 }, { 0, 15 }, { 1, 16 },
				{ 2, 14 }, { 2, 15 }, { 2, 16 } });
		map.put("005_Block", new int[][] { { 0, 0 }, { 0, 1 }, { 1, 0 }, { 1, 1 } });

		return map;
	}

	private void createSimulationSection(Composite parent, final GameOfLife3D model) {
		Composite section = new Composite(parent, SWT.NONE);
		section.setLayout(new GridLayout(1, false));
		section.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData sectionData = new GridData(SWT.FILL, SWT.TOP, true, false);
		section.setLayoutData(sectionData);

		Label header = new Label(section, SWT.NONE);
		header.setText("SIMULATION CONTROLS");
		header.setFont(boldFont16);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_CYAN));
		GridData headerData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		headerData.verticalIndent = 5;
		headerData.horizontalIndent = 10;
		header.setLayoutData(headerData);

		// Speed slider
		Composite speedRow = new Composite(section, SWT.NONE);
		speedRow.setLayout(new GridLayout(4, false));
		speedRow.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData speedRowData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		speedRowData.horizontalIndent = 10;
		speedRowData.verticalIndent = 5;
		speedRow.setLayoutData(speedRowData);

		Label speedLabel = new Label(speedRow, SWT.NONE);
		speedLabel.setText("Speed:");
		speedLabel.setFont(regularFont11);
		GridData speedLabelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		speedLabelData.widthHint = 80;
		speedLabel.setLayoutData(speedLabelData);

		Scale speedScale = new Scale(speedRow, SWT.HORIZONTAL);
		speedScale.setMinimum(1);
		speedScale.setMaximum(20);
		speedScale.setSelection(model.SPEED);
		GridData speedScaleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		speedScale.setLayoutData(speedScaleData);

		speedValue = new Label(speedRow, SWT.NONE);
		speedValue.setText(String.valueOf(model.SPEED));
		speedValue.setFont(regularFont11);
		speedValue.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_CYAN));
		GridData speedValueData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		speedValueData.widthHint = 40;
		speedValue.setLayoutData(speedValueData);

		Label speedUnit = new Label(speedRow, SWT.NONE);
		speedUnit.setText("gen/f");
		speedUnit.setFont(regularFont11);
		GridData speedUnitData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		speedUnit.setLayoutData(speedUnitData);

		speedScale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = speedScale.getSelection();
				model.SPEED = value;
				speedValue.setText(String.valueOf(value));
			}
		});

		// Population slider
		Composite popRow = new Composite(section, SWT.NONE);
		popRow.setLayout(new GridLayout(4, false));
		popRow.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData popRowData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		popRowData.horizontalIndent = 10;
		popRowData.verticalIndent = 5;
		popRow.setLayoutData(popRowData);

		Label popLabel = new Label(popRow, SWT.NONE);
		popLabel.setText("Population:");
		popLabel.setFont(regularFont11);
		GridData popLabelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		popLabelData.widthHint = 80;
		popLabel.setLayoutData(popLabelData);

		Scale popScale = new Scale(popRow, SWT.HORIZONTAL);
		popScale.setMinimum(5);
		popScale.setMaximum(80);
		popScale.setSelection((int) (model.INITIAL_POPULATION * 100));
		GridData popScaleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		popScale.setLayoutData(popScaleData);

		populationValue = new Label(popRow, SWT.NONE);
		populationValue.setText((int) (model.INITIAL_POPULATION * 100) + "%");
		populationValue.setFont(regularFont11);
		populationValue.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_CYAN));
		GridData popValueData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		popValueData.widthHint = 40;
		populationValue.setLayoutData(popValueData);

		Label popUnit = new Label(popRow, SWT.NONE);
		popUnit.setFont(regularFont11);
		GridData popUnitData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		popUnit.setLayoutData(popUnitData);

		popScale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = popScale.getSelection();
				model.INITIAL_POPULATION = value / 100.0;
				populationValue.setText(value + "%");
				model.requestGridReset();
			}
		});
	}

	private void createGridSection(Composite parent, final GameOfLife3D model) {
		Composite section = new Composite(parent, SWT.NONE);
		section.setLayout(new GridLayout(1, false));
		section.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData sectionData = new GridData(SWT.FILL, SWT.TOP, true, false);
		section.setLayoutData(sectionData);

		Label header = new Label(section, SWT.NONE);
		header.setText("GRID SETTINGS");
		header.setFont(boldFont16);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		GridData headerData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		headerData.verticalIndent = 5;
		headerData.horizontalIndent = 10;
		header.setLayoutData(headerData);

		// Grid Width slider
		Composite widthRow = new Composite(section, SWT.NONE);
		widthRow.setLayout(new GridLayout(4, false));
		widthRow.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData widthRowData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		widthRowData.horizontalIndent = 10;
		widthRowData.verticalIndent = 5;
		widthRow.setLayoutData(widthRowData);

		Label widthLabel = new Label(widthRow, SWT.NONE);
		widthLabel.setText("Width:");
		widthLabel.setFont(regularFont11);
		GridData widthLabelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		widthLabelData.widthHint = 80;
		widthLabel.setLayoutData(widthLabelData);

		Scale widthScale = new Scale(widthRow, SWT.HORIZONTAL);
		widthScale.setMinimum(100);
		widthScale.setMaximum(2000);
		widthScale.setSelection(model.GRID_WIDTH);
		GridData widthScaleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		widthScale.setLayoutData(widthScaleData);

		gridWidthValue = new Label(widthRow, SWT.NONE);
		gridWidthValue.setText(String.valueOf(model.GRID_WIDTH));
		gridWidthValue.setFont(regularFont11);
		gridWidthValue.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		GridData widthValueData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		widthValueData.widthHint = 40;
		gridWidthValue.setLayoutData(widthValueData);

		Label widthUnit = new Label(widthRow, SWT.NONE);
		widthUnit.setText("px");
		widthUnit.setFont(regularFont11);
		GridData widthUnitData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		widthUnit.setLayoutData(widthUnitData);

		widthScale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = widthScale.getSelection();
				model.GRID_WIDTH = value;
				gridWidthValue.setText(String.valueOf(value));
				model.requestGridReset();
			}
		});

		// Grid Height slider
		Composite heightRow = new Composite(section, SWT.NONE);
		heightRow.setLayout(new GridLayout(4, false));
		heightRow.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData heightRowData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		heightRowData.horizontalIndent = 10;
		heightRowData.verticalIndent = 5;
		heightRow.setLayoutData(heightRowData);

		Label heightLabel = new Label(heightRow, SWT.NONE);
		heightLabel.setText("Height:");
		heightLabel.setFont(regularFont11);
		GridData heightLabelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		heightLabelData.widthHint = 80;
		heightLabel.setLayoutData(heightLabelData);

		Scale heightScale = new Scale(heightRow, SWT.HORIZONTAL);
		heightScale.setMinimum(100);
		heightScale.setMaximum(2000);
		heightScale.setSelection(model.GRID_HEIGHT);
		GridData heightScaleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		heightScale.setLayoutData(heightScaleData);

		gridHeightValue = new Label(heightRow, SWT.NONE);
		gridHeightValue.setText(String.valueOf(model.GRID_HEIGHT));
		gridHeightValue.setFont(regularFont11);
		gridHeightValue.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		GridData heightValueData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		heightValueData.widthHint = 40;
		gridHeightValue.setLayoutData(heightValueData);

		Label heightUnit = new Label(heightRow, SWT.NONE);
		heightUnit.setText("px");
		heightUnit.setFont(regularFont11);
		GridData heightUnitData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		heightUnit.setLayoutData(heightUnitData);

		heightScale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = heightScale.getSelection();
				model.GRID_HEIGHT = value;
				gridHeightValue.setText(String.valueOf(value));
				model.requestGridReset();
			}
		});
	}

	private void createZoomSection(Composite parent, final GameOfLife3D model) {
		Composite section = new Composite(parent, SWT.NONE);
		section.setLayout(new GridLayout(1, false));
		section.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData sectionData = new GridData(SWT.FILL, SWT.TOP, true, false);
		section.setLayoutData(sectionData);

		Label header = new Label(section, SWT.NONE);
		header.setText("ZOOM & PAN");
		header.setFont(boldFont16);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
		GridData headerData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		headerData.verticalIndent = 5;
		headerData.horizontalIndent = 10;
		header.setLayoutData(headerData);

		Label info = new Label(section, SWT.WRAP);
		info.setText("• Scroll wheel: Zoom in/out\n• Mouse drag: Pan\n• Double-click: Center view");
		info.setFont(regularFont11);
		GridData infoData = new GridData(SWT.FILL, SWT.TOP, true, false);
		infoData.horizontalIndent = 10;
		infoData.verticalIndent = 5;
		info.setLayoutData(infoData);

		// Zoom slider
		Composite zoomRow = new Composite(section, SWT.NONE);
		zoomRow.setLayout(new GridLayout(4, false));
		zoomRow.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData zoomRowData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		zoomRowData.horizontalIndent = 10;
		zoomRowData.verticalIndent = 5;
		zoomRow.setLayoutData(zoomRowData);

		Label zoomLabel = new Label(zoomRow, SWT.NONE);
		zoomLabel.setText("Zoom:");
		zoomLabel.setFont(regularFont11);
		GridData zoomLabelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		zoomLabelData.widthHint = 80;
		zoomLabel.setLayoutData(zoomLabelData);

		Scale zoomScale = new Scale(zoomRow, SWT.HORIZONTAL);
		zoomScale.setMinimum(1);
		zoomScale.setMaximum(500);
		zoomScale.setSelection((int) (model.ZOOM * 10));
		GridData zoomScaleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		zoomScale.setLayoutData(zoomScaleData);

		zoomValue = new Label(zoomRow, SWT.NONE);
		zoomValue.setText(String.format("%.2f", model.ZOOM) + "x");
		zoomValue.setFont(regularFont11);
		zoomValue.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
		GridData zoomValueData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		zoomValueData.widthHint = 40;
		zoomValue.setLayoutData(zoomValueData);

		Label zoomUnit = new Label(zoomRow, SWT.NONE);
		zoomUnit.setFont(regularFont11);
		GridData zoomUnitData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		zoomUnit.setLayoutData(zoomUnitData);

		zoomScale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = zoomScale.getSelection();
				double newZoom = value / 10.0;
				model.ZOOM = newZoom;
				zoomValue.setText(String.format("%.2f", newZoom) + "x");
			}
		});
	}

	private void createPatternSection(Composite parent, final GameOfLife3D model) {
		Composite section = new Composite(parent, SWT.NONE);
		section.setLayout(new GridLayout(1, false));
		section.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData sectionData = new GridData(SWT.FILL, SWT.FILL, true, true);
		section.setLayoutData(sectionData);

		Label header = new Label(section, SWT.NONE);
		header.setText("PATTERN LIBRARY (5 EXAMPLES)");
		header.setFont(boldFont16);
		header.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA));
		GridData headerData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		headerData.verticalIndent = 5;
		headerData.horizontalIndent = 10;
		header.setLayoutData(headerData);

		patternTable = new Table(section, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.heightHint = 200;
		tableData.horizontalIndent = 10;
		patternTable.setLayoutData(tableData);
		patternTable.setHeaderVisible(true);
		patternTable.setLinesVisible(true);

		TableColumn nameCol = new TableColumn(patternTable, SWT.LEFT);
		nameCol.setText("Pattern");
		nameCol.setWidth(200);

		TableColumn descCol = new TableColumn(patternTable, SWT.LEFT);
		descCol.setText("Description");
		descCol.setWidth(250);

		for (String name : patterns.keySet()) {
			String desc = getPatternDescription(name);
			TableItem item = new TableItem(patternTable, SWT.NONE);
			item.setText(new String[] { name, desc });
		}
	}

	private void createButtonSection(Composite parent, final GameOfLife3D model) {
		Composite section = new Composite(parent, SWT.NONE);
		section.setLayout(new GridLayout(3, true));
		section.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData sectionData = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
		sectionData.verticalIndent = 10;
		sectionData.horizontalIndent = 10;
		section.setLayoutData(sectionData);

		Button loadButton = new Button(section, SWT.PUSH);
		loadButton.setText("Load Pattern");
		loadButton.setFont(regularFont11);
		GridData loadData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		loadData.heightHint = 35;
		loadButton.setLayoutData(loadData);
		loadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = patternTable.getSelectionIndex();
				if (index >= 0) {
					TableItem item = patternTable.getItem(index);
					String selectedPattern = item.getText(0);
					if (patterns.containsKey(selectedPattern)) {
						model.loadPattern(patterns.get(selectedPattern));
					}
				}
			}
		});

		Button resetButton = new Button(section, SWT.PUSH);
		resetButton.setText("Random Grid");
		resetButton.setFont(regularFont11);
		GridData resetData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		resetData.heightHint = 35;
		resetButton.setLayoutData(resetData);
		resetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.initializeGrid();
			}
		});

		Button clearButton = new Button(section, SWT.PUSH);
		clearButton.setText("Clear All");
		clearButton.setFont(regularFont11);
		GridData clearData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		clearData.heightHint = 35;
		clearButton.setLayoutData(clearData);
		clearButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.clearAllCells();
			}
		});
	}

	private String getPatternDescription(String name) {
		if (name.contains("Glider_Gun"))
			return "Creates infinite glider stream";
		if (name.contains("Glider") && !name.contains("Gun"))
			return "Classic diagonal spaceship";
		if (name.contains("LWSS"))
			return "Horizontal spaceship";
		if (name.contains("Two_Gliders"))
			return "Two interacting gliders";
		if (name.contains("Block"))
			return "Stable still life";
		return "Pattern";
	}
}