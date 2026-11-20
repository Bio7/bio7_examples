# WindowBuilder JavaScript examples

Simulations as HTML pages with JavaScript, designed with the WindowBuilder:

| Example | The design (GUI class) | The page |
|---|---|---|
| Epidemic (the SIR model) | `src/webgui/EpidemicGui.java` | `web/EpidemicGui/` |
| Forest fire (a cellular automaton) | `src/webgui/ForestFireGui.java` | `web/ForestFireGui/` |
| Boids in 3D (WebGL: a `GLCanvas` in the design) | `src/webgui/BoidsGlGui.java` | `web/BoidsGlGui/` |
| Food chain in 3D (WebGL: a `Canvas` named `gl...`) | `src/wbgl/FoodChainGlGui.java` | `web/FoodChainGlGui/` |
| All widgets (each widget and its HTML counterpart) | `src/wbwidgets/AllWidgetsGui.java` | `web/AllWidgetsGui/` |

- **Open a page:** `index.html` in the browser of Bio7 or in any browser.
- **Change the design:** open the GUI class with the WindowBuilder (Open With >
  WindowBuilder Editor), then use the context menu of the class: Create JavaScript App.
  `index.html` is created again from the design; `app.js` (the simulation) and
  `style.css` stay.
- **The ids** of the HTML elements are the names of the variables of the GUI class.

All widgets, layouts and rules: `WindowBuilderApps.md` (in the project).
