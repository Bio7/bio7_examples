package wbwidgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/*
 * All widgets of the WindowBuilder which WindowBuilder Shiny and WindowBuilder
 * JavaScript convert, in one design (open it with the WindowBuilder: Open With >
 * WindowBuilder Editor). The same class is converted into two apps (context menu
 * > WindowBuilder Apps):
 *
 *   shiny/AllWidgetsGui   Create Shiny App       (ui.R from this class, server.R)
 *   web/AllWidgetsGui     Create JavaScript App  (index.html from this class, app.js)
 *
 * Each label in the design names the widget and what it becomes (Shiny / HTML).
 * The rules:
 * - The id of an element is the name of the variable (input$scaleValue in R,
 *   document.getElementById("scaleValue") in JavaScript).
 * - A Label or Text whose name starts with "out" is an output (written by the
 *   server or the JavaScript code), other Labels are text.
 * - Radio buttons of one parent are one group (the id: the Group).
 * - The layouts: GridLayout (rows and columns, spans), FillLayout, RowLayout.
 * - A Canvas is a plot (renderPlot) or a canvas (drawn with JavaScript).
 */
public class AllWidgetsGui extends Composite {

	/* Inputs */
	private Scale scaleValue;
	private Slider sliderValue;
	private Spinner spinnerCount;
	private Spinner spinnerRate;
	private Text txtName;
	private Text txtNotes;
	private Text txtSecret;
	private DateTime dateDay;
	private DateTime timeOfDay;
	/* Choices */
	private Combo comboColor;
	private List listSpecies;
	private List listHabitats;
	private Button chkActive;
	private Button tglMode;
	private Group grpSize;
	private Button rbSmall;
	private Button rbMedium;
	private Button rbLarge;
	/* Buttons */
	private Button btnPress;
	private Label outPressed;
	/* Outputs */
	private Canvas canvasPlot;
	private Table tableValues;
	private Label outText;
	private Text outValues;
	private Browser browserInfo;
	/* Layouts */
	private Composite cmpFill;
	private Composite cmpRow;
	private Button chkOne;
	private Button chkTwo;
	private SashForm sashForm;
	private CTabFolder ctabFolder;

	public AllWidgetsGui(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));

		Label lblIntro = new Label(this, SWT.WRAP);
		lblIntro.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		lblIntro.setText("All widgets which WindowBuilder Shiny and WindowBuilder JavaScript convert. Each label: the SWT widget -> Shiny / HTML. The values of all inputs are shown in the tab Outputs.");

		btnPress = new Button(this, SWT.NONE);
		btnPress.setText("Button (push) -> actionButton / <button>");

		outPressed = new Label(this, SWT.NONE);
		outPressed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		outPressed.setText("Label outPressed -> textOutput / <span>");

		TabFolder tabFolder = new TabFolder(this, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		/* ---------------- Inputs */
		TabItem tbtmInputs = new TabItem(tabFolder, SWT.NONE);
		tbtmInputs.setText("Inputs");

		Composite cmpInputs = new Composite(tabFolder, SWT.NONE);
		tbtmInputs.setControl(cmpInputs);
		cmpInputs.setLayout(new GridLayout(2, false));

		Label lblScale = new Label(cmpInputs, SWT.NONE);
		lblScale.setText("Scale -> sliderInput / <input type=range>");

		scaleValue = new Scale(cmpInputs, SWT.NONE);
		scaleValue.setSelection(50);
		scaleValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSlider = new Label(cmpInputs, SWT.NONE);
		lblSlider.setText("Slider -> sliderInput / <input type=range>");

		sliderValue = new Slider(cmpInputs, SWT.NONE);
		sliderValue.setMaximum(200);
		sliderValue.setIncrement(5);
		sliderValue.setSelection(30);
		sliderValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSpinner = new Label(cmpInputs, SWT.NONE);
		lblSpinner.setText("Spinner -> numericInput / <input type=number>");

		spinnerCount = new Spinner(cmpInputs, SWT.BORDER);
		spinnerCount.setSelection(10);
		spinnerCount.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSpinnerDigits = new Label(cmpInputs, SWT.NONE);
		lblSpinnerDigits.setText("Spinner with digits (2) -> decimals");

		spinnerRate = new Spinner(cmpInputs, SWT.BORDER);
		spinnerRate.setDigits(2);
		spinnerRate.setMaximum(1000);
		spinnerRate.setSelection(125);
		spinnerRate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblText = new Label(cmpInputs, SWT.NONE);
		lblText.setText("Text -> textInput / <input type=text>");

		txtName = new Text(cmpInputs, SWT.BORDER);
		txtName.setText("Bio7");
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblNotes = new Label(cmpInputs, SWT.NONE);
		lblNotes.setText("Text (SWT.MULTI) -> textAreaInput / <textarea>");

		txtNotes = new Text(cmpInputs, SWT.BORDER | SWT.MULTI);
		txtNotes.setMessage("Several lines");
		GridData gdNotes = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gdNotes.heightHint = 50;
		txtNotes.setLayoutData(gdNotes);

		Label lblSecret = new Label(cmpInputs, SWT.NONE);
		lblSecret.setText("Text (SWT.PASSWORD) -> passwordInput / <input type=password>");

		txtSecret = new Text(cmpInputs, SWT.BORDER | SWT.PASSWORD);
		txtSecret.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblDate = new Label(cmpInputs, SWT.NONE);
		lblDate.setText("DateTime (SWT.DATE) -> dateInput / <input type=date>");

		dateDay = new DateTime(cmpInputs, SWT.BORDER | SWT.DATE);

		Label lblTime = new Label(cmpInputs, SWT.NONE);
		lblTime.setText("DateTime (SWT.TIME) -> textInput / <input type=time>");

		timeOfDay = new DateTime(cmpInputs, SWT.BORDER | SWT.TIME);

		/* ---------------- Choices */
		TabItem tbtmChoices = new TabItem(tabFolder, SWT.NONE);
		tbtmChoices.setText("Choices");

		Composite cmpChoices = new Composite(tabFolder, SWT.NONE);
		tbtmChoices.setControl(cmpChoices);
		cmpChoices.setLayout(new GridLayout(2, false));

		Label lblCombo = new Label(cmpChoices, SWT.NONE);
		lblCombo.setText("Combo -> selectInput / <select>");

		comboColor = new Combo(cmpChoices, SWT.READ_ONLY);
		comboColor.setItems(new String[] { "Blue", "Red", "Green" });
		comboColor.select(0);
		comboColor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblList = new Label(cmpChoices, SWT.NONE);
		lblList.setText("List -> selectInput (a list) / <select size>");

		listSpecies = new List(cmpChoices, SWT.BORDER | SWT.SINGLE);
		listSpecies.setItems(new String[] { "Fox", "Rabbit", "Deer", "Owl" });
		listSpecies.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblListMulti = new Label(cmpChoices, SWT.NONE);
		lblListMulti.setText("List (SWT.MULTI) -> selectInput(multiple) / <select multiple>");

		listHabitats = new List(cmpChoices, SWT.BORDER | SWT.MULTI);
		listHabitats.setItems(new String[] { "Forest", "Meadow", "Lake", "City" });
		listHabitats.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblCheck = new Label(cmpChoices, SWT.NONE);
		lblCheck.setText("Button (SWT.CHECK) -> checkboxInput / <input type=checkbox>");

		chkActive = new Button(cmpChoices, SWT.CHECK);
		chkActive.setSelection(true);
		chkActive.setText("Active");

		Label lblToggle = new Label(cmpChoices, SWT.NONE);
		lblToggle.setText("Button (SWT.TOGGLE) -> checkboxInput / <input type=checkbox>");

		tglMode = new Button(cmpChoices, SWT.TOGGLE);
		tglMode.setText("Expert mode");

		Label lblRadio = new Label(cmpChoices, SWT.NONE);
		lblRadio.setText("Button (SWT.RADIO) in a Group -> radioButtons / <input type=radio>");

		grpSize = new Group(cmpChoices, SWT.NONE);
		grpSize.setText("Group -> wellPanel / <fieldset>");
		grpSize.setLayout(new RowLayout(SWT.HORIZONTAL));

		rbSmall = new Button(grpSize, SWT.RADIO);
		rbSmall.setText("Small");

		rbMedium = new Button(grpSize, SWT.RADIO);
		rbMedium.setSelection(true);
		rbMedium.setText("Medium");

		rbLarge = new Button(grpSize, SWT.RADIO);
		rbLarge.setText("Large");

		/* ---------------- Outputs */
		TabItem tbtmOutputs = new TabItem(tabFolder, SWT.NONE);
		tbtmOutputs.setText("Outputs");

		Composite cmpOutputs = new Composite(tabFolder, SWT.NONE);
		tbtmOutputs.setControl(cmpOutputs);
		cmpOutputs.setLayout(new GridLayout(2, false));

		Label lblCanvas = new Label(cmpOutputs, SWT.NONE);
		lblCanvas.setText("Canvas -> plotOutput / <canvas>");

		canvasPlot = new Canvas(cmpOutputs, SWT.BORDER);
		GridData gdPlot = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gdPlot.widthHint = 500;
		gdPlot.heightHint = 250;
		canvasPlot.setLayoutData(gdPlot);

		Label lblTable = new Label(cmpOutputs, SWT.NONE);
		lblTable.setText("Table -> tableOutput / <table>");

		tableValues = new Table(cmpOutputs, SWT.BORDER | SWT.FULL_SELECTION);
		tableValues.setHeaderVisible(true);
		tableValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		Label lblOutText = new Label(cmpOutputs, SWT.NONE);
		lblOutText.setText("Label outText -> textOutput / <span>");

		outText = new Label(cmpOutputs, SWT.NONE);
		outText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		outText.setText("(a text of the server)");

		Label lblOutValues = new Label(cmpOutputs, SWT.NONE);
		lblOutValues.setText("Text outValues -> verbatimTextOutput / <pre>");

		outValues = new Text(cmpOutputs, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
		GridData gdValues = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gdValues.heightHint = 120;
		outValues.setLayoutData(gdValues);

		Label lblBrowser = new Label(cmpOutputs, SWT.NONE);
		lblBrowser.setText("Browser -> uiOutput / <div> (HTML)");

		browserInfo = new Browser(cmpOutputs, SWT.NONE);
		GridData gdBrowser = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gdBrowser.heightHint = 60;
		browserInfo.setLayoutData(gdBrowser);

		/* ---------------- Layouts */
		TabItem tbtmLayouts = new TabItem(tabFolder, SWT.NONE);
		tbtmLayouts.setText("Layouts");

		Composite cmpLayouts = new Composite(tabFolder, SWT.NONE);
		tbtmLayouts.setControl(cmpLayouts);
		cmpLayouts.setLayout(new GridLayout(1, false));

		Label lblGrid = new Label(cmpLayouts, SWT.WRAP);
		lblGrid.setText("GridLayout -> fluidRow and column / a CSS grid (all tabs use it; a span: the width of several columns)");

		Label lblFill = new Label(cmpLayouts, SWT.NONE);
		lblFill.setText("FillLayout (SWT.HORIZONTAL) -> columns of equal width / a flex row:");

		cmpFill = new Composite(cmpLayouts, SWT.NONE);
		cmpFill.setLayout(new FillLayout(SWT.HORIZONTAL));
		cmpFill.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblFillA = new Label(cmpFill, SWT.BORDER | SWT.CENTER);
		lblFillA.setText("A");

		Label lblFillB = new Label(cmpFill, SWT.BORDER | SWT.CENTER);
		lblFillB.setText("B");

		Label lblFillC = new Label(cmpFill, SWT.BORDER | SWT.CENTER);
		lblFillC.setText("C");

		Label lblRowLayout = new Label(cmpLayouts, SWT.NONE);
		lblRowLayout.setText("RowLayout -> flowLayout / a flex row which wraps:");

		cmpRow = new Composite(cmpLayouts, SWT.NONE);
		cmpRow.setLayout(new RowLayout(SWT.HORIZONTAL));

		chkOne = new Button(cmpRow, SWT.CHECK);
		chkOne.setText("One");

		chkTwo = new Button(cmpRow, SWT.CHECK);
		chkTwo.setText("Two");

		Label lblSash = new Label(cmpLayouts, SWT.NONE);
		lblSash.setText("SashForm (SWT.HORIZONTAL) -> columns / a flex row:");

		sashForm = new SashForm(cmpLayouts, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblLeft = new Label(sashForm, SWT.BORDER);
		lblLeft.setText("Left part");

		Label lblRight = new Label(sashForm, SWT.BORDER);
		lblRight.setText("Right part");

		Label lblCTab = new Label(cmpLayouts, SWT.NONE);
		lblCTab.setText("CTabFolder (and TabFolder) -> tabsetPanel / tabs:");

		ctabFolder = new CTabFolder(cmpLayouts, SWT.BORDER);
		ctabFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		CTabItem tbtmFirst = new CTabItem(ctabFolder, SWT.NONE);
		tbtmFirst.setText("First");

		Label lblFirst = new Label(ctabFolder, SWT.NONE);
		lblFirst.setText("The content of the first tab");
		tbtmFirst.setControl(lblFirst);

		CTabItem tbtmSecond = new CTabItem(ctabFolder, SWT.NONE);
		tbtmSecond.setText("Second");

		Label lblSecond = new Label(ctabFolder, SWT.NONE);
		lblSecond.setText("The content of the second tab");
		tbtmSecond.setControl(lblSecond);

		Link lnkBio7 = new Link(cmpLayouts, SWT.NONE);
		lnkBio7.setText("Link -> HTML: <a href=\"https://bio7.org\">bio7.org</a>");
	}
}
