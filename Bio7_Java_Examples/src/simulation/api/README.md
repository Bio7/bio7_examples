# The simulation API of Bio7: examples of all methods

Each example of this folder uses all methods of one class of the package `com.eco.bio7.simulation` and explains them in comments. Compile an example in Bio7 (it runs with the Play/Pause button) and export it with **Export Simulation as HTML...** (the same model runs in the web browser).

| Example | Class | Display in Bio7 |
|---|---|---|
| [ApiBasics](ApiBasics.java) | `Simulation` (the methods of all models), `@Param`, `@Action` | ImageJ |
| [ApiGrid](ApiGrid.java) | `GridModel`: cells with integer states | ImageJ |
| [ApiValueGrid](ApiValueGrid.java) | `ValueGridModel`: cells with double values | ImageJ |
| [ApiAgents](ApiAgents.java) | `AgentModel`, `Agent`: agents in a 3D box | 3D view |
| [ApiGeoGrid](ApiGeoGrid.java) | `GeoGridModel`: a georeferenced grid with data of GDAL | ImageJ (and the WorldWind globe) |
| [ApiGeoAgents](ApiGeoAgents.java) | `GeoAgentModel`, `GeoAgent`: agents on the Earth | WorldWind globe |

## Life cycle

| Method | Meaning |
|---|---|
| `init()` | The start state: after the compilation, the Setup button and a restart setting |
| `step()` | One time step (the Play/Pause button calls it repeatedly) |
| `t` | The number of the time step |
| `load()` | Only `GeoGridModel`: reads the data with GDAL (once, before the first init; not in the HTML page) |
| `settings(Composite)` | An own settings panel in Bio7 (e.g., of the WindowBuilder), see `Epidemic` and `EpidemicSettings` |

## Settings and buttons (ApiBasics)

| Annotation | Control |
|---|---|
| `@Param(min, max, step)` on a number | A slider |
| `@Param` on a number without min and max | A number field |
| `@Param` on a boolean | A checkbox |
| `@Param(options = {...})` on a String | A choice |
| `@Param` on a String without options | A text field |
| `label`, `group`, `restart = true` | The label, a heading above the settings of the group, init() again after a change |
| `@Action(label, group)` on a public method without parameters | A button |

## Methods of all models (ApiBasics)

| Group | Methods |
|---|---|
| Random numbers | `random()`, `random(min, max)`, `randomInt(n)`, `randomInt(min, max)`, `chance(p)`, `randomGaussian()`, `randomGaussian(mean, sd)`, `randomExponential(rate)`, `randomPoisson(lambda)`, `randomElement(list)`, `shuffle(list)`, `seed(n)` |
| Random numbers in parallel threads | `randomAt(index)`, `randomAt(index, k)`: from the seed, `t` and the index (the same in any order of the threads) |
| Mathematics | `clamp`, `lerp`, `mapRange`, `wrapAround`, `sq`, `distance`, `distance3`, `logistic`, `logisticGrowth` |
| Statistics | `sum`, `mean`, `variance`, `sd`, `min`, `max` (of `double[]`; `sum` and `mean` also of `int[]`) |
| Differential equations | `rk4((t, y) -> new double[] {...}, t, y, dt)`: one Runge-Kutta step |
| Chart | `chart(name, value)`: one value per step (a chart in the HTML page); `chartColors(chart, colors...)`: the colors of the series (in the order of their first values) or of the bars (one per bar), e.g. the colors of the map |
| Custom perspective | `perspectiveLabel(label)`: the name of the Custom perspective in the perspective switcher (e.g., the name of the model; null: "Custom"), `perspectiveIcon(file)`: its icon (an SVG or PNG file or a `platform:/plugin/...` URI; null: the simulation icon, which a model shows by default) |

## Images, 3D models, shapes and charts (all models)

| Group | Methods |
|---|---|
| Images and 3D models | `loadImage(name, file)` (PNG with transparency, JPEG), `loadModel(name, file)` (glTF `.glb`/`.gltf`, also skinned and animated; OBJ `.obj` with the materials and textures of its `.mtl`, y up; STL `.stl`, binary or ASCII, z up; the HTML export converts OBJ and STL to glTF); relative paths in the project; the HTML export embeds them |
| Agents | `Agent`: `image` (a billboard), `model` (a glTF model), `animation` (its name or number, `"none"`: still), `heading` (degrees, 0 the direction of y, clockwise; `NaN`: the direction of the movement), `size`; `GeoAgent`: `image`, `model` (its height: `agentSizeMeters`), `animation`; `agentDistance(a, b)` (as `distance(a, b)`; the same name on grids), `cameraFollow(index)` (the camera of the 3D view follows the agent, -1: off; the same as the field `followAgent`) |
| Shapes (drawn again in each step) | `drawLine`, `drawLine3` (3D), `drawPath`, `drawPolygon`, `drawCircle`, `drawRect`, `drawText`, `drawImage(name, x, y, size, angle)`; colors `#rrggbb` or `#rrggbbaa` (null: none); the coordinates of the model: cells (grids), the box (agents, 2D shapes on the floor), longitude and latitude (geospatial agents: the radius in km, the size of texts and images in pixels); `drawPillar(x, y, radius, height, color)`, `drawCone(x, y, radius, height, color)` (from the floor or the terrain); an agent model: a last argument `z` (the height above the floor) for `drawLine`, `drawPath`, `drawPolygon`, `drawCircle`, `drawRect`, `drawText`, `drawImage`; a grid: `drawTextOverlay`, `drawRectOverlay` (an ImageJ overlay, sharp at each zoom) |
| 3D shapes (an agent model, lit; on the globe of a geospatial model: `drawPillar`, `drawCone`, `drawSphere` and `drawPoint3` (a shaded ball, its diameter in model units, the fastest for thousands, e.g. a plant in each cell)) | `drawPoint3(x, y, z, size, color)` (a marker), `drawPath3(xs, ys, zs, color, width)`, `drawBox(x, y, z, w, h, d, fill, stroke)` (the corner, the depth d upwards), `drawSphere(x, y, z, radius, fill)`, `drawPolygon3(xs, ys, zs, fill, stroke)` (a flat polygon, e.g. a roof); a fill `#rrggbbaa` is transparent |
| Charts | `chart(name, value)` (the first chart), `chart(chart, name, value)` (a second, third, ... chart with a title), `chartXY(chart, name, x, y)` (e.g., a phase plot), `chartBars(chart, labels, values)` (replaced in each call); Bio7 shows them in the Charts view below the display |
| Mathematics | `dot`, `norm`, `matMul`, `matVec`, `transpose`, `inverse`, `solve`, `determinant`, `linearRegression` (intercept, slope, r²), `correlation`, `quantile`, `median`, `normalPdf`, `normalCdf`, `noise`, `fractalNoise`, `cosDeg`, `sinDeg`, `headingOf`, `randomBinomial`, `randomWeighted` (all also in the HTML export with the same results) |
| Experiments (batch runs) | `new Experiment(ModelClass.class)` or `new Experiment(model)` (its settings), `.param(name, values...)`, `.paramValues(name, values...)` (all combinations), `.steps(n)`, `.repetitions(n)` (the seeds seed, seed + 1, ...), `.seed(s)`, `.every(n)`, `.threads(n)` (parallel), `.run()`: an `Experiment.Table` (a row for each recorded step of each run: run, seed, parameters, step, the values of the line charts and of `record(name, value)`), `.save("file.csv")`, `.toR("name")` (a data frame in R), `.csv()`, `.column(name)`; in Bio7 also the button "Experiment..." of the settings (any model); `.design(names, rows)`: given parameter sets (`String[]`, `double[][]`, e.g. created in R or Python), each run with the repetitions, the column `point` is the row of the design (0, 1, ...) |
| Stop and seed | `stop()` in `step()` (e.g., `if (infected == 0) stop();`): Bio7 pauses, the page stops, an experiment ends the run; `isStopped()`; the seed of the setup: the field **Seed** of the settings and of the page (`getStartSeed()`, `setStartSeed(seed)`; `seed(...)` in `init()` has priority) |
| Mouse and keyboard | override `click(double x, double y)` (a click), `drag(double x, double y)` (a drag: the start and each further position; a grid: each new cell), `key(String key)` (a character as `"a"` or a special key: `"ArrowLeft"`, `"ArrowRight"`, `"ArrowUp"`, `"ArrowDown"`, `"Enter"`, `"Escape"`, `"Backspace"`, `"Delete"`, `"Tab"`); x, y: a grid in cells from the top left (`(int) x`, `(int) y`), an agent model the point on the floor of the box. Called between the steps (also when paused), the view is updated afterwards. In Bio7 the ImageJ image (a drag with the left button) and the 3D view (a click without a movement, Shift + drag; a drag without Shift rotates the view); the HTML export the same. A model with `key` gets the keys of the view (not ImageJ or the 3D view) |
| Agents on grids (GridModel, ValueGridModel, GeoGridModel) | `GridAgent` (`x`, `y` in cells, `heading`, `color`, `image`, `size`, `shape`: `"circle"`, `"arrow"`, `"square"`; `cellX()`, `cellY()`; extend it with own fields), `agents`, `addAgent(a)`, `removeAgent(a)`, `forward(a, d)`, `moveTo(a, x, y)`, `face(a, x, y)` (a torus or the edge: `wrap`), `agentDistance(a, b)`, `agentsAt(x, y)`, `countAgentsAt(x, y)`, `agentsNear(a, r)`; drawn on the grid in Bio7 and in the page; Setup removes them |
| Spatial index (agent models) | `index(cellSize)`: a `SpatialIndex` of the positions now; `.neighbors(agent, radius)`, `.nearest(agent, radius)`, `.near(x, y, z, radius)`, `.forEachPair(radius, (a, b) -> ...)` (fast for thousands of agents, the results in the order of the list); `neighborPairs(distance)`: all pairs closer than the distance as indices of the list `i0, j0, i1, j1, ...` (i < j; as `neighbor_pairs` of Python and R) |
| Networks (agent models) | `link(a, b)`, `link(a, b, color)`, `unlink`, `removeLinks(a)`, `clearLinks()`, `linked(a, b)`, `linkNeighbors(a)`, `degree(a)`, `setLinkColor`, `getLinks()` (pairs of indices), `linkCount()`; `linkRandom(p)`, `linkSmallWorld(k, p)`, `linkScaleFree(m)`, `linkNearby(radius)`; `components()`, `shortestPath(a, b)`, `clustering(a)`; `layoutCircle()`, `layoutSpring(iterations)`; `linkColor`, `linkWidth` (the links are drawn as lines in the 3D view and the page); `links()` (the same as `getLinks()`: pairs of indices `i0, j0, ...`), `linkCount()` |

The examples SheepWolvesGrid (a discrete model with images), SheepWolvesField (a continuous model with images, shapes and all kinds of charts), Pasture3D (animated glTF models in the 3D view), AlpineSheep (glTF models on the WorldWind globe) NetworkEpidemic (an epidemic on a network: links, a spatial index, recorded values for experiments, stop(), a click infects a person), HeatDiffusion (click, drag and key) and Ants (agents on a grid of pheromone) use them. The libraries of Bio7 (Apache Commons Math, Jama, Parallel Colt, JOML, JTS) can be used in Bio7, but not in the HTML export.

## GridModel (ApiGrid)

`size(w, h)`, `width`, `height`, `cells`, `next`, `wrap`, `palette(colors...)`, `get(x, y)`, `set(x, y, state)`, `setNext(x, y, state)`, `swap()`, `countNeighbors(x, y, state)` (8 neighbors), `count4(x, y, state)` (4 neighbors), `count(state)`.

## ValueGridModel (ApiValueGrid)

`size(w, h)`, `width`, `height`, `values`, `next`, `wrap`, `range(min, max)`, `colorMap(colors...)`, `VIRIDIS`, `get`, `set`, `setNext`, `swap`, `sum4(x, y)`, `sum8(x, y)`, `laplacian(x, y)`, `mean()`.

## AgentModel (ApiAgents)

`Agent` (`x`, `y`, `z`, `color`), `agents`, `box(w, h, d)`, `boxWidth`, `boxHeight`, `boxDepth`, `agentRadius`, `agentColor`, `distance(a, b)`, `neighbors(agent, radius)`, `nearest(agent, radius)`, `keepInBox(agent)`, `wrapInBox(agent)`.

## GeoGridModel (ApiGeoGrid)

In `load()`: `grid(file, columns)`, `raster(file)`, `raster(file, band, resampling)`, `vector(file, value)`, `vector(file, attribute)`, `addLayer(name, values)`, `attribution(text)`, `background(name, file)` (a map under the grid: an RGB raster, e.g. a satellite image or a drawn map), `showOnGlobe()`, `globeOutline(color, meters)` (a fence along the border of the area on the globe; 0: a line on the ground).

In `init()` and `step()`: `layer(name)`, `showValues(values, min, max, colors...)`, `showStates()`, `showBackground(name)` (null: none; colors `#rrggbbaa` show the map below the cells), `backgroundNames()`, `cellShape("oval")` (the cells as ovals over the map; `"square"`), `globeGrid(false)` (only the shapes on the globe, e.g. a 3D plant in each cell), `colAt(lon, lat)`, `rowAt(lon, lat)`, `lon(x)`, `lat(y)`, `gradient(layer, x, y)`, `slopeDegrees(layer, x, y)`, `value(layer, x, y)`, `cellSizeMeters`, `originX`, `originY`, `cellWidth`, `cellHeight`, `south`, `west`, `north`, `east`, `crs` and all methods of a `GridModel`.

## GeoAgentModel (ApiGeoAgents)

`GeoAgent` (`lon`, `lat`, `color`, `size`, `label`, `altitude`, `heading`), `agents`, `area(south, west, north, east)`, `agentSize`, `agentColor`, `agentShape` (`"point"`, `"sphere"`, `"cone"`, `"box"`, `"cylinder"`), `agentSizeMeters`, `viewPitch`, `distanceKm(a, b)`, `distanceKm(lon1, lat1, lon2, lat2)`, `move(agent, eastKm, northKm)`, `moveTowards(agent, lon, lat, km)`, `neighborsKm(agent, km)`, `cosLat`, `cosDeg`, `sinDeg`, `EARTH_RADIUS`.

## The same results in Bio7 and in the browser

The export translates the model to JavaScript. The random numbers, the integer arithmetic and the functions `cosLat`, `cosDeg`, `sinDeg` and the random distributions give the same digits in Java and in the browser. `Math.sin`, `Math.cos`, `Math.exp` and `Math.log` of the model and `distanceKm` use the functions of Java or of the browser: the last digits can differ.

The translation supports a subset of Java: fields, methods (no overloading), static nested classes, the statements (no try/catch), arrays, `Math`, lambdas, `ArrayList`, `HashMap`, `String` methods, `IntStream.range(...).parallel().forEach(...)` (a loop in the browser) and `System.out.println` (the console of the browser). Not supported: `java.util.Random` (use `random()`), `Math.random()`, anonymous classes, enums and other libraries. The errors are marked in the editor.
