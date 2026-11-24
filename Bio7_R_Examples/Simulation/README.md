# Simulation models in R (the functions of bio7sim)

The simulation API of Bio7 for R: a model is an environment created with `grid_model()`, `value_grid_model()`, `agent_model()`, `geo_grid_model()` or `geo_agent_model()`. Its functions `m$init` (the start state) and `m$step` (one time step) get the model as argument. `simulation(m)` at the end of the script starts the model in Bio7: a grid in ImageJ, agents in the 3D view, geospatial models on the WorldWind globe, the settings on the left (the Custom perspective). The Play/Pause button runs the steps, Setup calls `m$init` again.

**Export Simulation as HTML...** (the context menu of the R editor, the menu of the Play/Pause button or the Settings view) writes a page which runs the same script in the web browser with [webR](https://docs.r-wasm.org/webr/) (R in WebAssembly, loaded from the internet when the page is opened). The results are the same as in Bio7: the random numbers of R with the same seed (`set.seed(42)` before `m$init`), the same R code.

Source a script with the R editor of Bio7 (the embedded R). The functions need no package.

| Example | Model | Display in Bio7 |
|---|---|---|
| [game_of_life.R](game_of_life.R) | `grid_model`: cells with integer states | ImageJ |
| [heat_diffusion.R](heat_diffusion.R) | `value_grid_model`: cells with double values; the mouse and the keyboard (`m$click`, `m$drag`, `m$key`: hot spots, painting heat, cooling) | ImageJ |
| [epidemic.R](epidemic.R) | `grid_model` with all kinds of settings, groups and buttons | ImageJ |
| [swarm.R](swarm.R) | `agent_model`: boids in 3D (a data frame, matrices) | 3D view |
| [wildfire.R](wildfire.R) | `geo_grid_model`: elevation and land cover of GDAL (data in `data/geo`) | ImageJ and the WorldWind globe |
| [hot_air_balloons.R](hot_air_balloons.R) | `geo_agent_model`: OpenGL spheres on the globe | WorldWind globe |
| [sheep_wolves.R](sheep_wolves.R) | `value_grid_model`: sheep and wolves as images, shapes, four kinds of charts | ImageJ and the Charts view |
| [pasture_3d.R](pasture_3d.R) | `agent_model`: animated glTF models of sheep and wolves | 3D view |
| [ants.R](ants.R) | `value_grid_model` with agents on the grid (`m$agents`, arrows; all ants of a step as vectors): ants follow a pheromone which diffuses and evaporates and carry food home; `m$stop()` when all food is in the nest | ImageJ and the Charts view |
| [invasive_plant.R](invasive_plant.R) | `grid_model`: a neobiota, the Himalayan balsam (*Impatiens glandulifera*) along a river: explosive seeds, drift downstream, jumps along the road, a seed bank, the cover by the suitability of the habitats; a click pulls plants, a campaign | ImageJ and the Charts view |
| [plant_habitats.R](plant_habitats.R) | `grid_model`: twelve plant species settle in six habitats by their Ellenberg indicator values (L, F, R, N); clonal growth, seed dispersal, the shade of the beech, disturbance; a panel beside the map: the species which fit each habitat and which grow there | ImageJ and the Charts view |
| [zone_of_influence.R](zone_of_influence.R) | `value_grid_model`: the zone of influence model of plant competition (symmetric to completely asymmetric), self-thinning, the Gini coefficient of the sizes; a click plants a seedling | ImageJ and the Charts view |
| [network_epidemic.R](network_epidemic.R) | `agent_model`: an epidemic (SIR) on a network: moving contacts (`m$neighbor_pairs`, a spatial index) or a small-world, scale-free or random network (links, `m$layout_spring`); `m$record` for experiments; a click infects the nearest person (`m$click`) | 3D view and the Charts view |
| [network_epidemic_experiment.R](network_epidemic_experiment.R) | `experiment()`: batch runs of network_epidemic.R (infection rates, two networks, 5 repetitions), a data frame and a plot | the console and a plot |
| [network_epidemic_sensitivity.R](network_epidemic_sensitivity.R) | `experiment(design = ...)`: a sensitivity analysis of network_epidemic.R (a Latin hypercube of `lhs`, 40 parameter sets, 3 runs each), the mean and the 95 % confidence interval of each set and the partial rank correlations (`sensitivity::pcc`); installs the packages if missing | the console and a plot |
| [glider_town.R](glider_town.R) | `agent_model`: the flock of gliders flies along a route through a small town: houses, trees (OBJ) and a church tower (STL) placed with `m$place_model`; office towers, streets, a balloon, route markers and labels as 3D shapes (`m$draw_box`, `m$draw_polygon3`, `m$draw_path3`, `m$draw_sphere`, `m$draw_point3`, `m$draw_text` with `z`) | 3D view (split view) and the Charts view |
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
| Random numbers | `m$random(n = 1, min = 0, max = 1)`, `m$random_int(min, max, n = 1)` (both included), `m$chance(p, n = 1)`, `m$random_gaussian(n = 1, mean = 0, sd = 1)`, `m$random_exponential(rate, n = 1)`, `m$random_poisson(lambda, n = 1)`, `m$random_element(x)`, `m$shuffle(x)`, `m$seed(n)` (also `runif`, `rnorm`, `sample` of R); `m$random_at(index, k = 0)` (from the seed, `m$t` and the index, also a vector of indices: independent of the order of the calls; the same numbers as `randomAt` in Java for the same index) |
| Functions | `clamp`, `lerp`, `map_range`, `wrap_around`, `sq`, `distance`, `distance3`, `logistic`, `logistic_growth`, `rk4(f, t, y, dt)`, `cos_lat`, `cos_deg`, `sin_deg` (also as `m$` functions) |

## Images, 3D models, shapes and charts

| Group | Functions |
|---|---|
| Images and 3D models | `m$load_image(name, file)`, `m$load_model(name, file)` (glTF `.glb`/`.gltf`, also animated; OBJ `.obj` with the materials and textures of its `.mtl`, y up; STL `.stl`, binary or ASCII, z up; the HTML export converts OBJ and STL to glTF); the columns `image`, `model`, `animation`, `heading`, `size` of `m$agents` |
| Shapes (drawn again in each step) | `m$draw_line`, `m$draw_line3`, `m$draw_path(xs, ys)`, `m$draw_polygon(xs, ys, fill =, stroke =)`, `m$draw_circle`, `m$draw_rect`, `m$draw_text`, `m$draw_image(name, x, y, size, angle)` in the coordinates of the model (cells from 0 for the shapes); `m$draw_pillar(x, y, radius, height = NULL, color =)` (an agent model: a vertical cylinder from the floor, e.g. an obstacle); `z =` (an agent model: the height above the floor) for `m$draw_line`, `m$draw_path`, `m$draw_polygon`, `m$draw_circle`, `m$draw_rect`, `m$draw_text`, `m$draw_image` |
| 3D shapes (an agent model, lit) | `m$draw_point3(x, y, z, size = 1, color =)` (a marker), `m$draw_path3(xs, ys, zs, color =, width =)`, `m$draw_box(x, y, z, w, h, d, fill =, stroke = NULL)` (the corner, the depth d upwards), `m$draw_sphere(x, y, z, r, fill =)`, `m$draw_polygon3(xs, ys, zs, fill =, stroke = NULL)` (a flat polygon, e.g. a roof); a fill `#rrggbbaa` is transparent |
| Charts | `m$chart(name, value)` (the first chart), `m$chart(name, value, chart = "Title")` (another chart), `m$chart_xy(chart, name, x, y)`, `m$chart_bars(chart, labels, values)` |
| Mathematics | `noise(x, y)`, `fractal_noise(x, y, octaves)` (vectorized; the same values as in Java), `heading_of`, `cos_deg`, `sin_deg`, `linear_regression`, `normal_pdf`, `normal_cdf`, `m$random_binomial`, `m$random_weighted`; base R and its stats functions |
| Experiments (batch runs) | `experiment(m, params = list(name = c(values)), steps = 100, repetitions = 1, seed = 42, every = 1)`: all combinations, a data frame: run, seed, parameters, step, the values of the line charts and of `m$record(name, value)` (the settings of the model are restored); in Bio7 also the button "Experiment..." of the settings (a CSV file, the data frame `experiment`); `design =`: given parameter sets (a data frame or a list of columns, e.g. a Latin hypercube of `lhs` or the samples of `sensitivity`), each run with the repetitions, the column `point` is the row of the design (1, 2, ...); the statistics with R |
| Stop and seed | `m$stop()` in `m$step` (e.g. `if (infected == 0) m$stop()`): Bio7 pauses, the page stops, an experiment ends the run; the seed of the setup: the field **Seed** of the settings and of the page (`m$seed(...)` in `m$init` has priority); `m$is_stopped()` |
| Mouse and keyboard | functions of the model: `m$click <- function(m, x, y)` (a click), `m$drag <- function(m, x, y)` (a drag: the start and each further position; a grid: each new cell), `m$key <- function(m, key)` (a character as `"a"` or a special key: `"ArrowLeft"`, `"ArrowRight"`, `"ArrowUp"`, `"ArrowDown"`, `"Enter"`, `"Escape"`, `"Backspace"`, `"Delete"`, `"Tab"`); x, y: positions from 0 (a grid: the cell is the column `floor(x) + 1` and the row `floor(y) + 1` of `m$values`; an agent model: the point on the floor of the box). In Bio7 the ImageJ image (a drag with the left button) and the 3D view (a click without a movement, Shift + drag; a drag without Shift rotates the view), also when the simulation is paused; in the page the same. A model with `m$key` gets the keys of the view (not ImageJ or the 3D view) |
| Agents on grids (grid_model, value_grid_model, geo_grid_model) | `m$agents` (a data frame: `x`, `y` in cells from 0 as the shapes, `heading`, `color`, `image`, `size`, `shape`: `"circle"`, `"arrow"`, `"square"`, own columns), `m$add_agents(n, x =, y =, heading =, ..., own columns)`, `m$remove_agents(rows)`, `m$forward(rows, d)`, `m$move_to(rows, x, y)`, `m$face(rows, x, y)` (vectorized; a torus or the edge: `m$wrap`), `m$agent_distance(i, j)`, `m$cell_x(rows)`, `m$cell_y(rows)` (from 1 as `m$cells`), `m$agents_at(x, y)`, `m$count_agents_at(x, y)`, `m$agent_counts(rows)` (a matrix), `m$agents_near(i, r)`; drawn on the grid in Bio7 and in the page; Setup removes them |
| Spatial search | `pairs_within(points, radius)` (all pairs within the distance: a matrix with the columns i, j; fast for many points), `m$neighbor_pairs(radius)` (the agents) (the format of the languages: R a matrix with the columns i, j; Python two arrays i, j; Java `neighborPairs`: `i0, j0, i1, j1, ...`) |
| Networks (agent models) | `m$link(i, j, color = NA)` (rows, vectors), `m$unlink`, `m$remove_links(i)`, `m$remove_agents(rows)` (the links renumbered), `m$clear_links()`, `m$linked(i, j)`, `m$link_neighbors(i)`, `m$degree(i = NULL)`, `m$set_link_color`, `m$links()`; `m$link_random(p)`, `m$link_small_world(k, p)`, `m$link_scale_free(mm)`, `m$link_nearby(radius)`; `m$components()` (the group of each row), `m$shortest_path(i, j)`, `m$clustering(i = NULL)`; `m$layout_circle()`, `m$layout_spring(iterations)`; `m$link_color`, `m$link_width` (drawn as lines); `m$link_count()`; `m$agent_distance(i, j)` (as `m$distance(i, j)`; the same name on grids) |

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
