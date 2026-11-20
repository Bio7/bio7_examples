# WindowBuilder Apps: Shiny and JavaScript from a WindowBuilder design

Design a GUI with the WindowBuilder (an SWT `Composite`), then let Bio7 create a
**Shiny app** or an **HTML page with JavaScript** from it. The design is only read
(the code the WindowBuilder writes in the constructor), it is not run.

## Workflow

1. Create a project: toolbar "Create" > WindowBuilder Shiny App Project or
   WindowBuilder JavaScript App Project, or use any GUI class (a `Composite`).
2. Open the GUI class with Open With > WindowBuilder Editor and design the GUI.
3. Context menu of the class > **WindowBuilder Apps** > Create Shiny App or Create
   JavaScript App. The first time, Bio7 asks for the location of the app:
   - a new project (e.g. `MyGui_Shiny`, to edit and share the app on its own),
   - a folder in the project of the GUI class (`shiny/<class>`, `web/<class>`),
   - a folder of the workspace.

   Bio7 remembers the choice for the class; Change ... Location chooses again.
4. Change the design and create the app again: only the generated file is written again.

| | Generated (written each time) | Your code (written once) |
|---|---|---|
| Shiny | `ui.R` | `server.R` |
| JavaScript | `index.html` | `app.js`, `style.css` |

If the design gets new buttons or outputs without code, the dialog shows the code to add.

## The rules

- **Ids:** the id of an element is the name of its variable: `input$scaleValue` in R,
  `document.getElementById("scaleValue")` in JavaScript.
- **Outputs:** a `Label` or `Text` whose name starts with `out` is an output
  (`outSummary`); other labels are text.
- **Radio buttons** of one parent are one group; its id is the `Group` (else the first button).
- **Not converted** widgets become a comment, and the dialog names them.

## The widgets

| SWT widget (WindowBuilder) | Shiny | HTML / JavaScript |
|---|---|---|
| `Scale`, `Slider` (minimum, maximum, selection, increment) | `sliderInput` | `<input type="range">` with its value |
| `Spinner` (with digits: decimals) | `numericInput` | `<input type="number">` |
| `Text` | `textInput` | `<input type="text">` |
| `Text` with `SWT.MULTI` | `textAreaInput` | `<textarea>` |
| `Text` with `SWT.PASSWORD` | `passwordInput` | `<input type="password">` |
| `Text` named `out...` | `verbatimTextOutput` | `<pre>` |
| `Combo` (items, select) | `selectInput` | `<select>` |
| `List` (`SWT.MULTI`: several) | `selectInput` (a list, `multiple`) | `<select size>` (`multiple`) |
| `Button` with `SWT.CHECK` or `SWT.TOGGLE` | `checkboxInput` | `<input type="checkbox">` |
| `Button` with `SWT.RADIO` | `radioButtons` (the group) | `<input type="radio">` (the group) |
| `Button` (push) | `actionButton` (counts the clicks) | `<button>` (click event) |
| `DateTime` (`SWT.DATE`) | `dateInput` | `<input type="date">` |
| `DateTime` (`SWT.TIME`) | `textInput` (hh:mm) | `<input type="time">` |
| `Label` | text | `<label>` |
| `Label` named `out...` | `textOutput` | `<span>` |
| `Link` | `HTML` | the link as HTML |
| `Canvas` (size hints: the size; smaller only on narrow screens) | `plotOutput` (`renderPlot`) | `<canvas>` (drawn with JavaScript) |
| `GLCanvas` (or a `Canvas` named `gl...`) | `rgl::rglwidgetOutput` (3D with the R package rgl) | `<canvas>` with a WebGL context (`app.js` gets a WebGL start) |
| `Table` | `tableOutput` (`renderTable`) | `<table>` |
| `Browser` | `uiOutput` (`renderUI`) | `<div>` (HTML) |
| `Group` (text) | `wellPanel` with a title | `<fieldset>` with a legend |
| `Composite` | `div` | `<div>` |
| `TabFolder`, `CTabFolder` with tab items | `tabsetPanel` (input: the active tab) | tabs |
| `SashForm` | columns | a flex row or column |

**3D:** in the WindowBuilder, add a normal `Canvas` and name its variable `gl` and an
uppercase letter, a digit or `_` (Properties view > Variable: `glView`, `gl3d`; not
`globalMap`). In code, a `GLCanvas` works too. The Shiny app needs the R package rgl
(`install.packages("rgl")`).

## The layouts

| SWT layout | Shiny | HTML |
|---|---|---|
| `GridLayout` (columns, spans of `GridData`) | `fluidRow` and `column` (12 parts) | a CSS grid (a column with extra width: `1fr`) |
| `FillLayout` | columns of equal width (vertical: one below the other) | a flex row (or column) |
| `RowLayout` | `flowLayout` | a flex row which wraps |
| other or none | one below the other | one below the other |

Absolute positions (no layout) are not carried over: the pages are responsive.

## The examples

| Example | Design | Shiny | JavaScript |
|---|---|---|---|
| **All widgets** (this table, with the values of all inputs) | `src/wbwidgets/AllWidgetsGui.java` | `shiny/AllWidgetsGui` | `web/AllWidgetsGui` |
| Predator and prey | `src/shinygui/PredatorPreyShinyGui.java` | `shiny/PredatorPreyShinyGui` | |
| Population viability | `src/shinygui/PopulationViabilityShinyGui.java` | `shiny/PopulationViabilityShinyGui` | |
| Epidemic (SIR) | `src/webgui/EpidemicGui.java` | | `web/EpidemicGui` |
| Forest fire | `src/webgui/ForestFireGui.java` | | `web/ForestFireGui` |
| Boids in 3D (WebGL) | `src/webgui/BoidsGlGui.java` | | `web/BoidsGlGui` |
| **Food chain in 3D** (a `Canvas` named `glAttractor`: rgl and WebGL from one design) | `src/wbgl/FoodChainGlGui.java` | `shiny/FoodChainGlGui` | `web/FoodChainGlGui` |

The example **All widgets** uses one design for both apps: each label of the design
names the widget and what it becomes; the tab Outputs shows the values of all inputs
and their types in R and JavaScript (e.g., a slider gives a number in R and a string
in JavaScript, a multiple list a vector or an array).
