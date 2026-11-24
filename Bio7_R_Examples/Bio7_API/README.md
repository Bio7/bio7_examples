# The Bio7 API for R

Examples for the API of Bio7 in the embedded R: ImageJ images, macros and the
results table, the embedded Python and the Java API of Bio7, ImageJ and Java. The
functions are in the attached environment "bio7" (as the module `bio7` of the
embedded Python). `bio7_api()` prints an overview.

Start R in Bio7 (embedded R) and run the scripts in the R editor or with
`source()` in the console.

| Example | Shows |
|---|---|
| 01_ImageJ_Images.R | `bio7_image()`, matrix processing in R, `bio7_show_image()` |
| 02_ImageJ_Macros_and_Results.R | `bio7_ij_macro()`, `bio7_ij_run()`, `bio7_results_table()`, `bio7_roi()` |
| 03_R_and_Python.R | `bio7_to_python()`, `bio7_python()`: data frames, matrices, dicts |
| 04_Java_API.R | `J()`, `$new()`, methods and fields with `$`, collections, overlays |
| 05_Browser_and_HTML.R | `bio7_browse()`: an HTML report in the browser view |
| 06_3D_Plots.R | 3D plots (OpenGL) in the 3D view: scatter, surface with contours, 2D histogram bars, line, colored points with a wireframe, pie, HTML export (WebGL) |
| 07_3D_Dynamic_Plots.R | Dynamic 3D plots: an evolving histogram and growing line graphs updated in each step (`plot = p`), random walkers recorded as HTML animation (`bio7_plot3d_recorder()`), box plot, violin, area, radar, rose and treemap charts |
| 08_Progress_and_Jobs.R | The progress of R as Bio7 jobs: `bio7_progress()`, named tasks with `bio7_task()`, an unknown total, stopping a long calculation |

## The functions

| Function | |
|---|---|
| `bio7_image(title = NULL)` | the current (or named) ImageJ image: matrix (height x width), stack array (slices, height, width), RGB array (height, width, 3) |
| `bio7_show_image(x, title, range)` | a matrix or array as ImageJ image (`range = c(min, max)` or `"auto"`) |
| `bio7_ij_macro(code, argument)` | an ImageJ macro (the value of its return statement) |
| `bio7_ij_macro_file(file, argument)` | an ImageJ macro file |
| `bio7_ij_run(command, options)` | an ImageJ command on the current image |
| `bio7_results_table()` | the ImageJ results table as data frame |
| `bio7_roi()` | the selection of the current image (`x`, `y`) |
| `bio7_browse(url)` | a URL or HTML file in the browser view |
| `bio7_progress(done, total = NULL, text = "")` | the progress of the running code: a job in the status bar and the Progress view, its stop button interrupts R (e.g., `bio7_progress(i, length(files), basename(f))` in a loop); `bio7_progress_end()` ends it |
| `bio7_task(text, total = NULL)` | a task with progress (a job of its own): `t$update()` for one step (`t$update(n, text)`), `t$done()` at the end |
| `bio7_python(code)` | Python code: the value of the last expression |
| `bio7_to_python(name, value)` | assigns an R value in Python |
| `J(class)` | a Java class: static methods and fields with `$`, `$new(...)` |
| `bio7_java_new(class, ...)` | a new Java object |
| `bio7_java_ui(x)` | the methods of a Java object or class in the UI thread |
| `is_java(x)` | `TRUE` for a Java object |
| `bio7_plot3d(..., title, xlab, ylab, zlab, log, xlim, ylim, zlim, aspect, legend, colorbar, font, html, show, plot, record)` | shows the layers as 3D plot in the 3D view (`log = "z"`, `html`: also as WebGL page); `plot = p` updates a shown plot (in each step, the camera is kept), `record = rec` adds a frame to a recorder |
| `bio7_scatter3d(x, y, z, label, color, size, values, colormap, alpha, drop_lines)` | points (`x` also a data frame or matrix with 3 columns; `values`: colored with the color map) |
| `bio7_line3d(x, y, z, label, color, alpha, filled)` | a line (`filled`: down to the floor) |
| `bio7_surface3d(x, y, z, label, colormap, color, alpha, mesh)` | a surface of the grid `z[i, j]` at `x[i]`, `y[j]` (a matrix as `outer(x, y, f)` or a function `f(x, y)`) |
| `bio7_wireframe3d(x, y, z, label, color, colormap, alpha, step)` | a wireframe of a grid |
| `bio7_bars3d(x, y, z, label, color, colormap, alpha, width)` | bars: `z` a vector (at the points) or a grid (e.g. a 2D histogram) |
| `bio7_contour3d(x, y, z, levels, on_floor, label, color, colormap, alpha)` | the contour lines of a grid |
| `bio7_pie3d(values, labels, colors, explode, height)` | a 3D pie chart (e.g. of a `table()`) |
| `bio7_plot3d_clear()` | removes the 3D plot |
| `bio7_histogram3d(x, names, bins, bin_width, density, colors)` | a histogram of values or of groups (a list or data frame: in the depth) |
| `bio7_linegraph3d(x, series, names, colors, markers, marker_size, fill)` | line graphs of series over `x` (a matrix with a series in each column, a data frame or a list) |
| `bio7_area3d(x, series, names, colors, stacked)` | an area chart of series over `x` |
| `bio7_boxplot3d(groups, names, colors, width, outliers)`, `bio7_violin3d(groups, names, colors, width, bandwidth, quartiles)` | box plots and violins of groups (a list, data frame or matrix) |
| `bio7_radar3d(series, axes, names, colors, max, rings)` | a radar chart (a matrix or data frame: a series in each row, an axis in each column) |
| `bio7_rose3d(values, sectors, classes, colors, area_proportional, height, gap)` | a rose chart (e.g. a wind rose; a matrix `[sector, class]` stacked) |
| `bio7_treemap3d(values, labels, colors, heights, show_values)` | a treemap (e.g. of a `table()`) |
| `bio7_plot3d_recorder(speed, loop, autoplay, max_frames)` | records plots (`bio7_plot3d(..., record = rec, show = FALSE)`) as animation |
| `bio7_plot3d_export(recorder, file, blender, open)` | the recording as HTML page (WebGL, Play/Pause, timeline), also for Blender |

Colors: R color names, `"#RRGGBB"` or numbers `0xRRGGBB`. Color maps: `"viridis"`,
`"rainbow"`, `"diverging"`, `"terrain"`, the name of an hcl palette of R (`hcl.pals()`,
e.g. `"Plasma"`) or a vector of colors.

Java arguments: an R number is converted to the parameter type (an integral number
also for `int`), a vector to an array, a character string to `String`. Java
arrays come back as vectors (row by row for 2-d arrays as matrix), Java objects
as proxies (`as.list()` for collections).
