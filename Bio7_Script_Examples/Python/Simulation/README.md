# Simulation models in Python (the module bio7sim)

The simulation API of Bio7 for Python: a model is a class which extends one of the model classes and implements `init()` (the start state) and `step()` (one time step). `simulation(MyModel)` at the end of the script starts the model in Bio7: a grid in ImageJ, agents in the 3D view, geospatial models on the WorldWind globe, the settings on the left (the Custom perspective). The Play/Pause button runs the steps, Setup calls `init()` again.

**Export Simulation as HTML...** (the menu of the Play/Pause button or the Settings view) writes a page which runs the same script in the web browser with [Pyodide](https://pyodide.org) (Python and numpy in WebAssembly, loaded from the internet when the page is opened). The results are the same as in Bio7: the random numbers of numpy with the same seed, the same numpy code.

Run a script with the Python editor of Bio7 (the embedded Python with numpy).

| Example | Class | Display in Bio7 |
|---|---|---|
| [game_of_life.py](game_of_life.py) | `GridModel`: cells with integer states | ImageJ |
| [heat_diffusion.py](heat_diffusion.py) | `ValueGridModel`: cells with double values | ImageJ |
| [epidemic.py](epidemic.py) | `GridModel` with all kinds of settings, groups and buttons | ImageJ |
| [swarm.py](swarm.py) | `AgentModel`: boids in 3D (numpy arrays) | 3D view |
| [wildfire.py](wildfire.py) | `GeoGridModel`: elevation and land cover of GDAL (data in `data/geo`) | ImageJ and the WorldWind globe |
| [hot_air_balloons.py](hot_air_balloons.py) | `GeoAgentModel`: OpenGL spheres on the globe | WorldWind globe |
| [sheep_wolves.py](sheep_wolves.py) | `ValueGridModel`: sheep and wolves as images, shapes, four kinds of charts | ImageJ and the Charts view |
| [pasture_3d.py](pasture_3d.py) | `AgentModel`: animated glTF models of sheep and wolves | 3D view |

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
| Random numbers | `self.random()`, `self.random(min, max)`, `self.random_int(n)`, `self.random_int(a, b)` (b excluded), `self.chance(p)`, `self.random_gaussian(mean, sd)`, `self.random_exponential(rate)`, `self.random_poisson(lam)`, `self.random_element(items)`, `self.shuffle(items)`, `self.random_array(shape, min, max)` (the shape of the grid if omitted), `self.rng` (a numpy `Generator`), `self.seed(n)` |
| Functions | `clamp`, `lerp`, `map_range`, `wrap_around`, `sq`, `distance`, `distance3`, `logistic`, `logistic_growth`, `mean`, `variance`, `sd`, `rk4(f, t, y, dt)` (also as `self.` methods) |

## Images, 3D models, shapes and charts

| Group | Methods |
|---|---|
| Images and 3D models | `self.load_image(name, file)`, `self.load_model(name, file)` (glTF `.glb`/`.gltf`, also animated); `Agent(..., image=, model=, animation=, heading=, size=)`, `GeoAgent(..., image=, model=, animation=)` |
| Shapes (drawn again in each step) | `self.draw_line`, `draw_line3`, `draw_path(xs, ys)`, `draw_polygon(xs, ys, fill=, stroke=)`, `draw_circle`, `draw_rect`, `draw_text`, `draw_image(name, x, y, size, angle)` in the coordinates of the model |
| Charts | `self.chart(name, value)` (the first chart), `self.chart(name, value, chart="Title")` (another chart), `self.chart_xy(chart, name, x, y)`, `self.chart_bars(chart, labels, values)` |
| Mathematics | `noise(x, y)`, `fractal_noise(x, y, octaves)` (also for arrays; the same values as in Java), `heading_of`, `cos_deg`, `sin_deg`, `linear_regression`, `normal_pdf`, `normal_cdf`, `self.random_binomial`, `self.random_weighted`; numpy and the modules of Pyodide (e.g., scipy: loaded by the page when the script imports them) |

## The model classes

| Class | Fields and methods |
|---|---|
| `GridModel` | `size(w, h)`, `width`, `height`, `cells` (a numpy array `cells[y, x]`), `wrap`, `palette(colors...)`, `get(x, y)`, `set(x, y, state)`, `count_neighbors(state)` (an array of the 8 neighbors; with `x, y` the number of one cell), `count4(state)`, `count(state)` |
| `ValueGridModel` | `size(w, h)`, `values` (a numpy array), `wrap`, `range(min, max)`, `color_map(colors...)`, `VIRIDIS`, `get`, `set`, `sum4()`, `sum8()`, `laplacian()` (arrays), `mean()` |
| `AgentModel`, `Agent` | `agents` (a list of `Agent(x, y, z, color)` and own subclasses), `box(w, h, d)`, `box_width`, `box_height`, `box_depth`, `agent_radius`, `agent_color`, `positions()` (an array (n, 3)), `distance(a, b)`, `neighbors(agent, r)`, `nearest(agent, r)`, `keep_in_box(agent)`, `wrap_in_box(agent)` |
| `GeoGridModel` | In `load()` (only in Bio7): `grid(file, columns)`, `raster(file, band=1, resampling="bilinear")`, `vector(file, value=1, attribute=None)`, `add_layer(name, values)`, `attribution(text)`, `show_on_globe()`. In `init()` and `step()`: `layer(name)`, `show_values(values, min, max, colors...)`, `show_states()`, `col_at(lon, lat)`, `row_at(lon, lat)`, `lon(x)`, `lat(y)`, `value(layer, x, y)`, `gradient(layer)`, `slope_degrees(layer)` (arrays), `cell_size_meters`, `south`, `west`, `north`, `east`, `crs` and all methods of a `GridModel` |
| `GeoAgentModel`, `GeoAgent` | `agents` (a list of `GeoAgent(lon, lat)`: `color`, `size`, `label`, `altitude`, `heading`), `area(south, west, north, east)`, `agent_size`, `agent_color`, `agent_shape` (`"point"`, `"sphere"`, `"cone"`, `"box"`, `"cylinder"`), `agent_size_meters`, `view_pitch`, `distance_km(a, b)` (or of `lon1, lat1, lon2, lat2`), `move(agent, east_km, north_km)`, `move_towards(agent, lon, lat, km)`, `neighbors_km(agent, km)`, `cos_lat`, `cos_deg`, `sin_deg`, `EARTH_RADIUS` |

The relative paths of the data (e.g., `data/geo/harz_dem.tif`) are searched from the folder of the script up to the folder of the project.

## The page

The page needs an internet connection (Pyodide, numpy and the map are loaded when it is opened; the first start takes a few seconds). The data of a geospatial model is embedded in the page (the layers loaded in Bio7). The script must not use other modules of Bio7 (e.g., `bio7`) in `init()` and `step()`. The arithmetic of numpy (`+ - * /`, `sqrt`, comparisons) gives the same digits in Bio7 and in the browser; transcendental functions (`np.exp`, `np.sin`, `math.log`) can differ in the last digit.
