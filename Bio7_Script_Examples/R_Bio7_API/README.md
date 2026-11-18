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
| `bio7_python(code)` | Python code: the value of the last expression |
| `bio7_to_python(name, value)` | assigns an R value in Python |
| `J(class)` | a Java class: static methods and fields with `$`, `$new(...)` |
| `bio7_java_new(class, ...)` | a new Java object |
| `bio7_java_ui(x)` | the methods of a Java object or class in the UI thread |
| `is_java(x)` | `TRUE` for a Java object |

Java arguments: an R number is converted to the parameter type (an integral number
also for `int`), a vector to an array, a character string to `String`. Java
arrays come back as vectors (row by row for 2-d arrays as matrix), Java objects
as proxies (`as.list()` for collections).
