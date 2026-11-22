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
| Chart | `chart(name, value)`: one value per step (a chart in the HTML page) |

## Images, 3D models, shapes and charts (all models)

| Group | Methods |
|---|---|
| Images and 3D models | `loadImage(name, file)` (PNG with transparency, JPEG), `loadModel(name, file)` (glTF `.glb`/`.gltf`, also skinned and animated); relative paths in the project; the HTML export embeds them |
| Agents | `Agent`: `image` (a billboard), `model` (a glTF model), `animation` (its name or number, `"none"`: still), `heading` (degrees, 0 the direction of y, clockwise; `NaN`: the direction of the movement), `size`; `GeoAgent`: `image`, `model` (its height: `agentSizeMeters`), `animation` |
| Shapes (drawn again in each step) | `drawLine`, `drawLine3` (3D), `drawPath`, `drawPolygon`, `drawCircle`, `drawRect`, `drawText`, `drawImage(name, x, y, size, angle)`; colors `#rrggbb` or `#rrggbbaa` (null: none); the coordinates of the model: cells (grids), the box (agents, 2D shapes on the floor), longitude and latitude (geospatial agents: the radius in km, the size of texts and images in pixels) |
| Charts | `chart(name, value)` (the first chart), `chart(chart, name, value)` (a second, third, ... chart with a title), `chartXY(chart, name, x, y)` (e.g., a phase plot), `chartBars(chart, labels, values)` (replaced in each call); Bio7 shows them in the Charts view below the display |
| Mathematics | `dot`, `norm`, `matMul`, `matVec`, `transpose`, `inverse`, `solve`, `determinant`, `linearRegression` (intercept, slope, r²), `correlation`, `quantile`, `median`, `normalPdf`, `normalCdf`, `noise`, `fractalNoise`, `cosDeg`, `sinDeg`, `headingOf`, `randomBinomial`, `randomWeighted` (all also in the HTML export with the same results) |

The examples SheepWolvesGrid (a discrete model with images), SheepWolvesField (a continuous model with images, shapes and all kinds of charts), Pasture3D (animated glTF models in the 3D view) and AlpineSheep (glTF models on the WorldWind globe) use them. The libraries of Bio7 (Apache Commons Math, Jama, Parallel Colt, JOML, JTS) can be used in Bio7, but not in the HTML export.

## GridModel (ApiGrid)

`size(w, h)`, `width`, `height`, `cells`, `next`, `wrap`, `palette(colors...)`, `get(x, y)`, `set(x, y, state)`, `setNext(x, y, state)`, `swap()`, `countNeighbors(x, y, state)` (8 neighbors), `count4(x, y, state)` (4 neighbors), `count(state)`.

## ValueGridModel (ApiValueGrid)

`size(w, h)`, `width`, `height`, `values`, `next`, `wrap`, `range(min, max)`, `colorMap(colors...)`, `VIRIDIS`, `get`, `set`, `setNext`, `swap`, `sum4(x, y)`, `sum8(x, y)`, `laplacian(x, y)`, `mean()`.

## AgentModel (ApiAgents)

`Agent` (`x`, `y`, `z`, `color`), `agents`, `box(w, h, d)`, `boxWidth`, `boxHeight`, `boxDepth`, `agentRadius`, `agentColor`, `distance(a, b)`, `neighbors(agent, radius)`, `nearest(agent, radius)`, `keepInBox(agent)`, `wrapInBox(agent)`.

## GeoGridModel (ApiGeoGrid)

In `load()`: `grid(file, columns)`, `raster(file)`, `raster(file, band, resampling)`, `vector(file, value)`, `vector(file, attribute)`, `addLayer(name, values)`, `attribution(text)`, `showOnGlobe()`.

In `init()` and `step()`: `layer(name)`, `showValues(values, min, max, colors...)`, `showStates()`, `colAt(lon, lat)`, `rowAt(lon, lat)`, `lon(x)`, `lat(y)`, `gradient(layer, x, y)`, `slopeDegrees(layer, x, y)`, `value(layer, x, y)`, `cellSizeMeters`, `originX`, `originY`, `cellWidth`, `cellHeight`, `south`, `west`, `north`, `east`, `crs` and all methods of a `GridModel`.

## GeoAgentModel (ApiGeoAgents)

`GeoAgent` (`lon`, `lat`, `color`, `size`, `label`, `altitude`, `heading`), `agents`, `area(south, west, north, east)`, `agentSize`, `agentColor`, `agentShape` (`"point"`, `"sphere"`, `"cone"`, `"box"`, `"cylinder"`), `agentSizeMeters`, `viewPitch`, `distanceKm(a, b)`, `distanceKm(lon1, lat1, lon2, lat2)`, `move(agent, eastKm, northKm)`, `moveTowards(agent, lon, lat, km)`, `neighborsKm(agent, km)`, `cosLat`, `cosDeg`, `sinDeg`, `EARTH_RADIUS`.

## The same results in Bio7 and in the browser

The export translates the model to JavaScript. The random numbers, the integer arithmetic and the functions `cosLat`, `cosDeg`, `sinDeg` and the random distributions give the same digits in Java and in the browser. `Math.sin`, `Math.cos`, `Math.exp` and `Math.log` of the model and `distanceKm` use the functions of Java or of the browser: the last digits can differ.

The translation supports a subset of Java: fields, methods (no overloading), static nested classes, the statements (no try/catch), arrays, `Math`, lambdas, `ArrayList`, `HashMap`, `String` methods, `IntStream.range(...).parallel().forEach(...)` (a loop in the browser) and `System.out.println` (the console of the browser). Not supported: `java.util.Random` (use `random()`), `Math.random()`, anonymous classes, enums and other libraries. The errors are marked in the editor.
