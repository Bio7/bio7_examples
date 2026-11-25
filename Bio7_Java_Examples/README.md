# Java examples

Bio7 compiles and runs the classes of this project (the packages in `src`).

| Package | Contents |
|---|---|
| `simulation` | The simulation API: grid, value grid, agent and geospatial models, 3D scenes, experiments, networks, agents on grids, the HTML export; plant ecology: `InvasivePlant` (a neobiota along a river), `PlantHabitats` (species and habitats by Ellenberg indicator values), `ZoneOfInfluence` (plant competition); `simulation.api` explains all methods (with a README) |
| `models` | Models in the 2D and 3D view: Game of Life, DLA, forest fire, artificial life, predator and prey, swarms, iso surfaces; `models.imagej` (ImageJ plugins: cellular automata, fractals, flocking), `models.math`, `models.random` |
| `html` | Models as HTML pages and recordings |
| `imagej` | ImageJ plugins |
| `r` | Java and embedded R: models analysed in R, R in actions, plots to ImageJ; `r.gui` (GUIs with R) |
| `flow.models` | The Java models of the flows in Bio7_Flow_Examples/Models (harvest, predator-prey, metapopulation, sea turtle matrix, wildlife disease): each `main` sends its results to R |
| `python` | Java and embedded Python; `python.gui`, `python.camera`, `python.templatematching` (GUIs with Python and OpenCV) |
| `opengl` | The 3D view: drawing, camera, textures and models, plots (`opengl.plot3d`, `opengl.plotapi`), measurements, physics |
| `gui` | `gui.swt` (SWT and Nebula widgets, dashboards), `gui.windowbuilder`, `gui.shiny`, `gui.web` (WindowBuilder designs as Shiny and JavaScript apps in the folders `shiny` and `web`, see WindowBuilderApps.md) |
| `geo` | `geo.gdal` (raster and vector data, terrain, habitat models), `geo.worldwind` (the globe) |
| `workbench` | The Bio7 workbench: editor colors, progress in the console |
| `deeplearning`, `arduino`, `profiling` | Deep learning segmentation, an Arduino on the serial port, profiling |

The folder `data` contains the images, 3D models and geodata of the simulations.

**Renamed packages** (earlier versions): `modeling`, `modelling`, `modeling3d` → `models`;
`gui` → `gui.swt`; `webgui` → `gui.web`; `shinygui` → `gui.shiny`; `wbwidgets`, `wbgl` →
`gui.windowbuilder`; `gdal` → `geo.gdal`; `worldwind` → `geo.worldwind`;
`extra` → `workbench`
and `r`.

**Removed** (they used classes which Bio7 no longer has): `FlowExampleJavaModel` (a flow with an old discrete model), `r.knitr` (a knitr report of a model in a flow with the old R API), `modelling.random.RandomWalk`, `opengl.collada.ColladaModel`, `extra.ChangeFontColors` and the ImageJ plugin `Forest_Fire_`.
