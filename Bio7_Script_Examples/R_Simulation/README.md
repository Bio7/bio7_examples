# Simulation models in R (the functions of bio7sim)

The simulation API of Bio7 for R: a model is an environment created with `grid_model()`, `value_grid_model()`, `agent_model()`, `geo_grid_model()` or `geo_agent_model()`. Its functions `m$init` (the start state) and `m$step` (one time step) get the model as argument. `simulation(m)` at the end of the script starts the model in Bio7: a grid in ImageJ, agents in the 3D view, geospatial models on the WorldWind globe, the settings on the left (the Custom perspective). The Play/Pause button runs the steps, Setup calls `m$init` again.

**Export Simulation as HTML...** (the context menu of the R editor, the menu of the Play/Pause button or the Settings view) writes a page which runs the same script in the web browser with [webR](https://docs.r-wasm.org/webr/) (R in WebAssembly, loaded from the internet when the page is opened). The results are the same as in Bio7: the random numbers of R with the same seed (`set.seed(42)` before `m$init`), the same R code.

Source a script with the R editor of Bio7 (the embedded R). The functions need no package.

| Example | Model | Display in Bio7 |
|---|---|---|
| [game_of_life.R](game_of_life.R) | `grid_model`: cells with integer states | ImageJ |
| [heat_diffusion.R](heat_diffusion.R) | `value_grid_model`: cells with double values | ImageJ |
| [epidemic.R](epidemic.R) | `grid_model` with all kinds of settings, groups and buttons | ImageJ |
| [swarm.R](swarm.R) | `agent_model`: boids in 3D (a data frame, matrices) | 3D view |
| [wildfire.R](wildfire.R) | `geo_grid_model`: elevation and land cover of GDAL (data in `data/geo`) | ImageJ and the WorldWind globe |
| [hot_air_balloons.R](hot_air_balloons.R) | `geo_agent_model`: OpenGL spheres on the globe | WorldWind globe |
| [sheep_wolves.R](sheep_wolves.R) | `value_grid_model`: sheep and wolves as images, shapes, four kinds of charts | ImageJ and the Charts view |
| [pasture_3d.R](pasture_3d.R) | `agent_model`: animated glTF models of sheep and wolves | 3D view |
| [gliders.R](gliders.R) | `agent_model`: a flock of gliders (triangles which climb, dive and bank), pillars as obstacles, a floor with shadows, a camera which follows a boid, the polarization of the flock | 3D view (split view) and the Charts view |

## Settings and buttons

```r
m$param("density", 0.3, min = 0, max = 1, step = 0.01, label = "Start density", restart = TRUE, group = "Start")
m$action("glider", function(m) { ... }, label = "Add a glider", group = "Cells")
```

| Setting | Control |
|---|---|
| `m$param(name, number, min =, max =, step =)` | A slider (whole numbers for whole values, min, max and step, else `type = "double"`) |
| `m$param(name, number)` | A number field (`7L`: an integer) |
| `m$param(name, TRUE)` | A checkbox |
| `m$param(name, "a", options = c("a", "b"))` | A choice |
| `m$param(name, "text")` | A text field |
| `label`, `group`, `restart = TRUE`, `type` | The label, a heading above the settings of the group, `m$init` again after a change, `"int"` or `"double"` |
| `m$action(name, function(m) ..., label =, group =)` | A button |

The value of a setting is `m$name` (e.g., `m$density`).

## All models

| Group | Functions |
|---|---|
| Life cycle | `m$init`, `m$step`, `m$t` (the step), `m$chart(name, value)` (one value per step) |
| Random numbers | `m$random(n = 1, min = 0, max = 1)`, `m$random_int(min, max, n = 1)` (both included), `m$chance(p, n = 1)`, `m$random_gaussian(n = 1, mean = 0, sd = 1)`, `m$random_exponential(rate, n = 1)`, `m$random_poisson(lambda, n = 1)`, `m$random_element(x)`, `m$shuffle(x)`, `m$seed(n)` (also `runif`, `rnorm`, `sample` of R) |
| Functions | `clamp`, `lerp`, `map_range`, `wrap_around`, `sq`, `distance`, `distance3`, `logistic`, `logistic_growth`, `rk4(f, t, y, dt)`, `cos_lat`, `cos_deg`, `sin_deg` (also as `m$` functions) |

## Images, 3D models, shapes and charts

| Group | Functions |
|---|---|
| Images and 3D models | `m$load_image(name, file)`, `m$load_model(name, file)` (glTF `.glb`/`.gltf`, also animated); the columns `image`, `model`, `animation`, `heading`, `size` of `m$agents` |
| Shapes (drawn again in each step) | `m$draw_line`, `m$draw_line3`, `m$draw_path(xs, ys)`, `m$draw_polygon(xs, ys, fill =, stroke =)`, `m$draw_circle`, `m$draw_rect`, `m$draw_text`, `m$draw_image(name, x, y, size, angle)` in the coordinates of the model (cells from 0 for the shapes); `m$draw_pillar(x, y, radius, height = NULL, color =)` (an agent model: a vertical cylinder from the floor, e.g. an obstacle) |
| Charts | `m$chart(name, value)` (the first chart), `m$chart(name, value, chart = "Title")` (another chart), `m$chart_xy(chart, name, x, y)`, `m$chart_bars(chart, labels, values)` |
| Mathematics | `noise(x, y)`, `fractal_noise(x, y, octaves)` (vectorized; the same values as in Java), `heading_of`, `cos_deg`, `sin_deg`, `linear_regression`, `normal_pdf`, `normal_cdf`, `m$random_binomial`, `m$random_weighted`; base R and its stats functions |

## The models

The grids are matrices `m$cells[y, x]` with the rows `y = 1..height` from the top and the columns `x = 1..width`; the coordinates of the functions are 1-based.

| Model | Fields and functions |
|---|---|
| `grid_model(name, description)` | `m$size(w, h)`, `m$width`, `m$height`, `m$cells` (an integer matrix), `m$wrap`, `m$palette(colors...)`, `m$get(x, y)`, `m$set(x, y, state)`, `m$count_neighbors(state)` (a matrix of the counts of the 8 neighbors), `m$count4(state)`, `m$count(state)` |
| `value_grid_model(...)` | `m$size(w, h)`, `m$values` (a matrix), `m$wrap`, `m$range(min, max)`, `m$color_map(colors...)`, `VIRIDIS`, `m$get`, `m$set`, `m$sum4()`, `m$sum8()`, `m$laplacian()` (matrices), `m$mean()` |
| `agent_model(...)` | `m$agents` (a data frame: `x`, `y`, `z`, `color` and own columns), `m$box(w, h, d)`, `m$box_width`, `m$box_height`, `m$box_depth`, `m$agent_radius`, `m$agent_color`, `m$agent_shape` (`"sphere"` or `"glider"`: triangles which point in the direction of the movement, climb, dive and bank into turns), `m$floor` (`"none"`, `"grid"`, `"plane"`), `m$shadows`, `m$camera_follow(i)` (the custom camera follows the agent of row `i`, only in Bio7; `NULL`: off), `m$place_model(model, x, y, z = 0, heading = 0, size = 0, animation = NULL)` (a 3D model of `m$load_model` in the environment, e.g. a house or a tree: no agent, it stays until `m$remove_model(i)` or `m$clear_models()`; Setup removes all), `m$distance(i, j)`, `m$neighbors(i, r)`, `m$nearest(i, r)` (rows), `m$keep_in_box()`, `m$wrap_in_box()` (all agents) |
| `geo_grid_model(...)` | In `m$load` (only in Bio7): `m$grid(file, columns)`, `m$raster(file, band = 1, resampling = "bilinear")`, `m$vector(file, value = 1, attribute = NULL)`, `m$add_layer(name, values)`, `m$attribution(text)`, `m$show_on_globe()`. In `m$init` and `m$step`: `m$layer(name)`, `m$show_values(values, min, max, colors...)`, `m$show_states()`, `m$col_at(lon, lat)`, `m$row_at(lon, lat)` (`NA` outside), `m$lon(x)`, `m$lat(y)`, `m$value(layer, x, y)`, `m$gradient(layer)`, `m$slope_degrees(layer)` (matrices), `m$cell_size_meters`, `m$south`, `m$west`, `m$north`, `m$east`, `m$crs` and all functions of a `grid_model` |
| `geo_agent_model(...)` | `m$agents` (a data frame: `lon`, `lat` and `altitude`, `heading`, `color`, `size`, `label`, own columns), `m$area(south, west, north, east)`, `m$agent_size`, `m$agent_color`, `m$agent_shape` (`"point"`, `"sphere"`, `"cone"`, `"box"`, `"cylinder"`), `m$agent_size_meters`, `m$view_pitch`, `m$distance_km(lon1, lat1, lon2, lat2)`, `m$move(i, east_km, north_km)`, `m$move_towards(i, lon, lat, km)`, `m$neighbors_km(i, km)` (rows `i`, vectorized), `EARTH_RADIUS` |

The relative paths of the data (e.g., `data/geo/harz_dem.tif`) are searched from the folder of the script up to the folder of the project.

## The page

The page needs an internet connection (webR and the map are loaded when it is opened; the first start takes a few seconds). The data of a geospatial model is embedded in the page (the layers loaded in Bio7). The script must not use packages or functions of Bio7 in `m$init` and `m$step` (base R works in webR). The vectorized arithmetic of R gives the same digits in Bio7 and in the browser; `sum()` and `mean()` of doubles, `exp()`, `log()` and `sin()` can differ in the last digit.
