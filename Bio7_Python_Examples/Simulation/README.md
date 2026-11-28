# Simulation models in Python (the module bio7sim)

The simulation API of Bio7 for Python: a model is a class which extends one of the model classes and implements `init()` (the start state) and `step()` (one time step). `simulation(MyModel)` at the end of the script starts the model in Bio7: a grid in ImageJ, agents in the 3D view, geospatial models on the WorldWind globe, the settings on the left (the Custom perspective). The Play/Pause button runs the steps, Setup calls `init()` again.

**Export Simulation as HTML...** (the menu of the Play/Pause button or the Settings view) writes a page which runs the same script in the web browser with [Pyodide](https://pyodide.org) (Python and numpy in WebAssembly, loaded from the internet when the page is opened). The results are the same as in Bio7: the random numbers of numpy with the same seed, the same numpy code.

Run a script with the Python editor of Bio7 (the embedded Python with numpy).

| Example | Class | Display in Bio7 |
|---|---|---|
| [game_of_life.py](game_of_life.py) | `GridModel`: cells with integer states | ImageJ |
| [heat_diffusion.py](heat_diffusion.py) | `ValueGridModel`: cells with double values; the mouse and the keyboard (`click`, `drag`, `key`: hot spots, painting heat, cooling) | ImageJ |
| [epidemic.py](epidemic.py) | `GridModel` with all kinds of settings, groups and buttons | ImageJ |
| [swarm.py](swarm.py) | `AgentModel`: boids in 3D (numpy arrays) | 3D view |
| [wildfire.py](wildfire.py) | `GeoGridModel`: elevation and land cover of GDAL (data in `data/geo`) | ImageJ and the WorldWind globe |
| [hot_air_balloons.py](hot_air_balloons.py) | `GeoAgentModel`: OpenGL spheres on the globe | WorldWind globe |
| [sheep_wolves.py](sheep_wolves.py) | `ValueGridModel`: sheep and wolves as images, shapes, four kinds of charts | ImageJ and the Charts view |
| [pasture_3d.py](pasture_3d.py) | `AgentModel`: animated glTF models of sheep and wolves | 3D view |
| [ants.py](ants.py) | `ValueGridModel` with agents on the grid (`GridAgent`, arrows): ants follow a pheromone which diffuses and evaporates and carry food home; `stop()` when all food is in the nest | ImageJ and the Charts view |
| [invasive_plant.py](invasive_plant.py) | `GridModel`: a neobiota, the Himalayan balsam (*Impatiens glandulifera*) along a river: explosive seeds, drift downstream, jumps along the road, a seed bank, the cover by the suitability of the habitats; a click pulls plants, a campaign | ImageJ and the Charts view |
| [plant_habitats.py](plant_habitats.py) | `GridModel`: twelve plant species settle in six habitats by their Ellenberg indicator values (L, F, R, N); clonal growth, seed dispersal, the shade of the beech, disturbance; a panel beside the map as an ImageJ overlay (`draw_text(..., overlay=True)`): the species which fit each habitat and which grow there | ImageJ and the Charts view |
| [zone_of_influence.py](zone_of_influence.py) | `ValueGridModel`: the zone of influence model of plant competition (symmetric to completely asymmetric), self-thinning, the Gini coefficient of the sizes; a click plants a seedling | ImageJ and the Charts view |
| [network_epidemic.py](network_epidemic.py) | `AgentModel`: an epidemic (SIR) on a network: moving contacts (`neighbor_pairs`, a spatial index) or a small-world, scale-free or random network (links, `layout_spring`); `record` for experiments; a click infects the nearest person (`click`) | 3D view and the Charts view |
| [network_epidemic_experiment.py](network_epidemic_experiment.py) | `experiment()`: batch runs of network_epidemic.py (infection rates, two networks, 5 repetitions) | the console |
| [network_epidemic_sensitivity.py](network_epidemic_sensitivity.py) | `experiment(design=...)`: a sensitivity analysis of network_epidemic.py with SALib (the method of Morris: 40 parameter sets, 3 runs each), the mean and the 95 % confidence interval of each set (pandas, scipy); requires SALib | the console and a plot |
| [glider_town.py](glider_town.py) | `AgentModel`: the flock of gliders flies along a route through a small town: houses, trees (OBJ) and a church tower (STL) placed with `place_model`; office towers, streets, a balloon, route markers and labels as 3D shapes (`draw_box`, `draw_polygon3`, `draw_path3`, `draw_sphere`, `draw_point3`, `draw_text` with `z`) | 3D view (split view) and the Charts view |
| [gliders.py](gliders.py) | `AgentModel`: a flock of gliders (triangles which climb, dive and bank), pillars as obstacles, a floor with shadows, a camera which follows a boid, the polarization of the flock | 3D view (split view) and the Charts view |
| [math_functions.py](math_functions.py) | `ValueGridModel`: the mathematical functions of bio7sim (`clamp`, `map_range`, `noise`, `fractal_noise`, `sin_deg`, `normal_pdf`, `rk4`, `linear_regression`, ...) with numpy (`np.median`, `np.quantile`, `np.linalg`); the built-in math of Python (the module `math`, `//`, `%`, `round`): a moving surface, statistics, a histogram, a phase plane, the logistic growth | ImageJ and the Charts view |

## Settings and buttons

```python
class MyModel(GridModel):
    density = param(0.3, min=0, max=1, step=0.01, label="Start density", restart=True, group="Start")

    @action("Add a glider", group="Cells")
    def glider(self):
        ...
```

| Setting | Control |
|---|---|
| `param(number, min=, max=, step=)` | A slider (an `int` start value: whole numbers) |
| `param(number)` | A number field |
| `param(True)` | A checkbox |
| `param("a", options=["a", "b"])` | A choice |
| `param("text")` | A text field |
| `label`, `group`, `restart=True` | The label, a heading above the settings of the group, `init()` again after a change |
| `@action("Label", group=)` on a method without parameters | A button |

## All models

| Group | Methods |
|---|---|
| Life cycle | `init()`, `step()`, `self.t` (the step), `self.chart(name, value)` (one value per step) |
| Random numbers | `self.random()`, `self.random(min, max)`, `self.random_int(n)`, `self.random_int(a, b)` (b excluded), `self.chance(p)`, `self.random_gaussian(mean, sd)`, `self.random_exponential(rate)`, `self.random_poisson(lam)`, `self.random_element(items)`, `self.shuffle(items)`, `self.random_array(shape, min, max)` (the shape of the grid if omitted), `self.rng` (a numpy `Generator`), `self.seed(n)`; `self.random_at(index, k=0)` (from the seed, `t` and the index, also an array of indices: independent of the order of the calls; the same numbers as `randomAt` in Java) |
| Functions | `clamp`, `lerp`, `map_range`, `wrap_around`, `sq`, `distance`, `distance3`, `logistic`, `logistic_growth`, `mean`, `variance`, `sd`, `rk4(f, t, y, dt)` (also as `self.` methods) |

## Images, 3D models, shapes and charts

| Group | Methods |
|---|---|
| Images and 3D models | `self.load_image(name, file)`, `self.load_model(name, file)` (glTF `.glb`/`.gltf`, also animated; OBJ `.obj` with the materials and textures of its `.mtl`, y up; STL `.stl`, binary or ASCII, z up; the HTML export converts OBJ and STL to glTF); `Agent(..., image=, model=, animation=, heading=, size=)`, `GeoAgent(..., image=, model=, animation=)` |
| Shapes (drawn again in each step) | `self.draw_line`, `draw_line3`, `draw_path(xs, ys)`, `draw_polygon(xs, ys, fill=, stroke=)`, `draw_circle`, `draw_rect`, `draw_text`, `draw_image(name, x, y, size, angle)` in the coordinates of the model; `draw_pillar(x, y, radius, height=None, color=)` (an agent model: a vertical cylinder from the floor, e.g. an obstacle); `z=` (an agent model: the height above the floor) for `draw_line`, `draw_path`, `draw_polygon`, `draw_circle`, `draw_rect`, `draw_text`, `draw_image`; `overlay=True` (a grid) for `draw_text` and `draw_rect`: an ImageJ overlay instead of the pixels of the image (sharp at each zoom, Image > Overlay > Hide Overlay; e.g. a description or a legend) |
| 3D shapes (an agent model, lit) | `draw_point3(x, y, z, size=1, color=)` (a marker), `draw_path3(xs, ys, zs, color=, width=)`, `draw_box(x, y, z, w, h, d, fill=, stroke=None)` (the corner, the depth d upwards), `draw_sphere(x, y, z, r, fill=)`, `draw_polygon3(xs, ys, zs, fill=, stroke=None)` (a flat polygon, e.g. a roof); a fill `#rrggbbaa` is transparent |
| Charts | `self.chart(name, value)` (the first chart), `self.chart(name, value, chart="Title")` (another chart), `self.chart_xy(chart, name, x, y)`, `self.chart_bars(chart, labels, values)` |
| Mathematics | `noise(x, y)`, `fractal_noise(x, y, octaves)` (also for arrays; the same values as in Java), `heading_of`, `cos_deg`, `sin_deg`, `linear_regression`, `normal_pdf`, `normal_cdf`, `self.random_binomial`, `self.random_weighted`; numpy and the modules of Pyodide (e.g., scipy: loaded by the page when the script imports them) |
| Experiments (batch runs) | `experiment(ModelClass or model, params={"name": [values]}, steps=100, repetitions=1, seed=42, every=1)`: all combinations, a pandas DataFrame (a list of dicts without pandas): run, seed, parameters, step, the values of the line charts and of `self.record(name, value)`; in Bio7 also the button "Experiment..." of the settings (a CSV file, a data frame in R); `design=`: given parameter sets (a DataFrame, a dict of columns or a list of dicts, e.g. a Latin hypercube or the samples of SALib), each run with the repetitions, the column `point` is the row of the design (0, 1, ...); the statistics with pandas, scipy or SALib |
| Stop and seed | `self.stop()` in `step()` (e.g. `if infected == 0: self.stop()`): Bio7 pauses, the page stops, an experiment ends the run; the seed of the setup: the field **Seed** of the settings and of the page (`self.seed(...)` in `init()` has priority); `self.is_stopped()` |
| Mouse and keyboard | methods of the model: `def click(self, x, y)` (a click), `def drag(self, x, y)` (a drag: the start and each further position; a grid: each new cell), `def key(self, key)` (a character as `"a"` or a special key: `"ArrowLeft"`, `"ArrowRight"`, `"ArrowUp"`, `"ArrowDown"`, `"Enter"`, `"Escape"`, `"Backspace"`, `"Delete"`, `"Tab"`); x, y: a grid in cells from the top left (`int(x)`, `int(y)`), an agent model the point on the floor of the box. In Bio7 the ImageJ image (a drag with the left button) and the 3D view (a click without a movement, Shift + drag; a drag without Shift rotates the view), also when the simulation is paused; in the page the same. A model with `key` gets the keys of the view (not ImageJ or the 3D view) |
| Agents on grids (GridModel, ValueGridModel, GeoGridModel) | `GridAgent(x, y, heading=0, color=, image=None, size=0.8, shape="circle")` (`"arrow"`, `"square"`; `cell_x()`, `cell_y()`; extend it), `self.agents`, `add_agent(a)`, `remove_agent(a)`, `forward(a, d)`, `move_to(a, x, y)`, `face(a, x, y)` (a torus or the edge: `wrap`), `agent_distance(a, b)`, `agents_at(x, y)`, `count_agents_at(x, y)`, `agents_near(a, r)`; drawn on the grid in Bio7 and in the page; Setup removes them |
| Spatial search | `pairs_within(points, radius)` (all pairs within the distance: arrays i, j; fast for many points), `self.neighbor_pairs(radius)` (the agents) (the format of the languages: Python two arrays i, j; R a matrix with the columns i, j; Java `neighborPairs`: `i0, j0, i1, j1, ...`) |
| Networks (agent models) | `self.link(a, b, color=None)`, `unlink`, `remove_links(a)`, `clear_links()`, `linked(a, b)`, `link_neighbors(a)`, `degree(a)`, `set_link_color`, `links()` (pairs of indices); `link_random(p)`, `link_small_world(k, p)`, `link_scale_free(m)`, `link_nearby(radius)`; `components()`, `shortest_path(a, b)`, `clustering(a)`; `layout_circle()`, `layout_spring(iterations)`; `link_color`, `link_width` (drawn as lines); `link_count()`; `agent_distance(a, b)` (as `distance(a, b)`; the same name on grids) |

## The model classes

| Class | Fields and methods |
|---|---|
| `GridModel` | `size(w, h)`, `width`, `height`, `cells` (a numpy array `cells[y, x]`), `wrap`, `palette(colors...)`, `get(x, y)`, `set(x, y, state)`, `count_neighbors(state)` (an array of the 8 neighbors; with `x, y` the number of one cell), `count4(state)`, `count(state)` |
| `ValueGridModel` | `size(w, h)`, `values` (a numpy array), `wrap`, `range(min, max)`, `color_map(colors...)`, `VIRIDIS`, `get`, `set`, `sum4()`, `sum8()`, `laplacian()` (arrays), `mean()` |
| `AgentModel`, `Agent` | `agents` (a list of `Agent(x, y, z, color)` and own subclasses), `box(w, h, d)`, `box_width`, `box_height`, `box_depth`, `agent_radius`, `agent_color`, `agent_shape` (`"sphere"` or `"glider"`: triangles which point in the direction of the movement, climb, dive and bank into turns), `floor` (`"none"`, `"grid"`, `"plane"`), `shadows`, `camera_follow(agent or index)` (the custom camera follows the agent, only in Bio7; `None`: off), `place_model(model, x, y, z=0, heading=0, size=0, animation=None)` (a 3D model of `load_model` in the environment, e.g. a house or a tree: no agent, it stays until `remove_model(index)` or `clear_models()`; Setup removes all), `positions()` (an array (n, 3)), `distance(a, b)`, `neighbors(agent, r)`, `nearest(agent, r)`, `keep_in_box(agent)`, `wrap_in_box(agent)` |
| `GeoGridModel` | In `load()` (only in Bio7): `grid(file, columns)`, `raster(file, band=1, resampling="bilinear")`, `vector(file, value=1, attribute=None)`, `add_layer(name, values)`, `attribution(text)`, `show_on_globe()`. In `init()` and `step()`: `layer(name)`, `show_values(values, min, max, colors...)`, `show_states()`, `col_at(lon, lat)`, `row_at(lon, lat)`, `lon(x)`, `lat(y)`, `value(layer, x, y)`, `gradient(layer)`, `slope_degrees(layer)` (arrays), `cell_size_meters`, `south`, `west`, `north`, `east`, `crs` and all methods of a `GridModel` |
| `GeoAgentModel`, `GeoAgent` | `agents` (a list of `GeoAgent(lon, lat)`: `color`, `size`, `label`, `altitude`, `heading`), `area(south, west, north, east)`, `agent_size`, `agent_color`, `agent_shape` (`"point"`, `"sphere"`, `"cone"`, `"box"`, `"cylinder"`), `agent_size_meters`, `view_pitch`, `distance_km(a, b)` (or of `lon1, lat1, lon2, lat2`), `move(agent, east_km, north_km)`, `move_towards(agent, lon, lat, km)`, `neighbors_km(agent, km)`, `cos_lat`, `cos_deg`, `sin_deg`, `EARTH_RADIUS` |

The relative paths of the data (e.g., `data/geo/harz_dem.tif`) are searched from the folder of the script up to the folder of the project.

## The page

The page needs an internet connection (Pyodide, numpy and the map are loaded when it is opened; the first start takes a few seconds). The data of a geospatial model is embedded in the page (the layers loaded in Bio7). The script must not use other modules of Bio7 (e.g., `bio7`) in `init()` and `step()`. The arithmetic of numpy (`+ - * /`, `sqrt`, comparisons) gives the same digits in Bio7 and in the browser; transcendental functions (`np.exp`, `np.sin`, `math.log`) can differ in the last digit.
