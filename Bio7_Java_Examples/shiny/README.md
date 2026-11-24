# WindowBuilder Shiny examples

Simulations as Shiny apps, designed with the WindowBuilder:

| Example | The design (GUI class) | The app |
|---|---|---|
| Predator and prey (Lotka-Volterra with logistic prey growth) | `src/gui/shiny/PredatorPreyShinyGui.java` | `shiny/PredatorPreyShinyGui/` |
| Population viability (stochastic births and deaths, Monte Carlo) | `src/gui/shiny/PopulationViabilityShinyGui.java` | `shiny/PopulationViabilityShinyGui/` |
| All widgets (each widget and its Shiny counterpart) | `src/gui/windowbuilder/AllWidgetsGui.java` | `shiny/AllWidgetsGui/` |
| Food chain in 3D (a `Canvas` named `gl...`: rgl; needs `install.packages("rgl")`) | `src/gui/windowbuilder/FoodChainGlGui.java` | `shiny/FoodChainGlGui/` |

- **Start an app:** open its `ui.R` or `server.R` and press "Start Shiny App" in the toolbar
  (R must run in the Bio7 console).
- **Change the design:** open the GUI class with the WindowBuilder (Open With >
  WindowBuilder Editor), then use the context menu of the class: Create Shiny App.
  `ui.R` is created again from the design; `server.R` (the simulation) stays.
- **The ids** of Shiny are the names of the variables of the GUI class (`input$scaleR`).

All widgets, layouts and rules: `WindowBuilderApps.md` (in the project).
