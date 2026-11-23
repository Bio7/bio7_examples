# Embedded Python

Examples for the embedded Python of Bio7: CPython runs in the process of Bio7
(Java FFM API), the data is shared with ImageJ and R and matplotlib draws into
the "Python Plot" view beside the console.

Setup: Preferences > Python: "Use Python (instead Jython)" and "Embedded
Python" (the defaults), select the Python folder or press "Detect Python...".
Start Python with the Python button of the main toolbar (or the console).

| Example | Shows |
|---|---|
| 01_Modern_Plots.py | matplotlib figures in the plot view (resizable, save, copy, ImageJ) |
| 02_ImageJ_Image_Processing.py | `bio7.image()`, numpy processing, `bio7.show_image()` |
| 03_Python_and_R.py | `bio7.to_r()`, `bio7.r()`: data frames, models, matrices |
| 04_Results_Table.py | `bio7.results_table()` as pandas DataFrame |
| 05_Deep_Learning_Segmentation.py | ImageJ image → deep learning segmentation (StarDist or Cellpose) → label, object and outline images in ImageJ |
| 06_KMeans_Color_Segmentation.py | k-means clustering of the colors (scikit-learn): labels and cluster colors in ImageJ |
| 07_PCA_Channels_Stack.py | PCA of the channels of an RGB image or a stack: component images and explained variance |
| 08_Watershed_Touching_Objects.py | Distance transform and watershed separate touching objects (scikit-image) |
| 09_Object_Measurements_to_R.py | Object measurements as pandas DataFrame, plots, the table in R |
| 10_Fourier_Filtering.py | FFT: power spectrum, low, high and band pass filters |
| 11_Edge_Detection.py | Sobel, Scharr, Prewitt, Laplacian of Gaussian and Canny compared |
| 12_Texture_GLCM.py | Texture features (GLCM) of tiles as feature maps |
| 13_Random_Forest_Pixel_Classification.py | Pixel classification with a random forest (multi-scale features) |
| 14_Stack_Projections_Profiles.py | Mean, maximum and standard deviation projections, intensity profile of a stack |
| 15_Object_Clustering_PCA_KMeans.py | Objects clustered by shape and intensity (PCA, k-means), colored in ImageJ |
| 16_Seaborn_Object_Statistics.py | Object statistics with seaborn: pair plot, distributions, correlations |
| 17_Plotly_Interactive_Image.py | Interactive plotly charts in the Browser view: intensity surface, objects with hover |
| 18_Plotnine_Grammar_of_Graphics.py | The grammar of graphics (as ggplot2) with plotnine for the objects of an image |
| 19_ImageJ_Macros_from_Python.py | ImageJ macros from Python (`bio7.ij_macro`, `bio7.ij_run`): return values, arguments, images |
| 20_ImageJ_API_from_Python.py | The ImageJ API (Java) with `bio7.java`: processors, threshold, particle analyzer, results table, numpy pixels |
| 21_ROIs_Overlays_Java_API.py | ROIs and overlays with the Java API, a polygon from numpy, Java collections and the Java library |
| 22_Progress_and_Jobs.py | The progress of Python as Bio7 jobs: `bio7.progress`, `bio7.task`, tqdm, stopping a running script |
| 23_Classes_Predator_Prey.py | An object-oriented program: several classes (inheritance) and a main class, a predator-prey model with a plot (Outline view, debugger, profiler) |
| 24_Classes_in_Files/main.py | The same model with one class per file (modules of a folder): run main.py |
| 25_3D_Plots.py | 3D plots (OpenGL) in the 3D view: `bio7.plot3d()` with `scatter3d`, `surface3d`, `contour3d`, `bars3d` (2D histogram), `line3d`, `wireframe3d`, `pie3d`, HTML export (WebGL) |
| 26_3D_Dynamic_Plots.py | Dynamic 3D plots: an evolving histogram and growing line graphs updated in each step (`plot=p`), random walkers recorded as HTML animation (`bio7.plot3d_recorder()`), `boxplot3d`, `violin3d`, `area3d`, `radar3d`, `rose3d`, `treemap3d` |

The Java API (`from bio7 import java`): `java.ij.IJ` (or `java.type("ij.IJ")`) is a class, calling it
creates an object, its static methods and fields are attributes. Java objects are proxies with their
methods and fields; numbers, strings and arrays are converted (Java arrays as numpy arrays, `byte[]`
and `short[]` as uint8/uint16 as the pixels of ImageJ). `java.ui(obj).method()` calls a method in the
UI thread (SWT widgets).
| Python_API.groovy | The Java API `PythonUtil` (toPython, fromPython, imageToPython, pythonToR, rToPython) |

The examples 06-15 use the current ImageJ image (without an open image a sample image of scikit-image).
They need numpy, scipy, scikit-image, scikit-learn, pandas and matplotlib (Python Packages: the bundles
"Scientific" and "Images", plus scikit-learn).

The Image-Methods view transfers ImageJ data to Python if "Python" is selected
in its toolbar (numpy arrays and pandas DataFrames).

More examples:

* `../OpenCV`: OpenCV (`cv2`) with ImageJ: filters, objects, videos as stacks, live video, tracking, optical flow, alignment.
* `../Markdown_Python`: Python chunks (```` ```{python} ````) in the markdown editor.
* `Bio7_Java_Examples/src/python`: Java <-> Python (`JavaToPython`, `PythonCallsJava`, `ImageJToPythonAndBack`).
* `Bio7_Java_Examples/src/r`: Java <-> embedded R with `RUtil` (FFM bridge): `JavaToR`, `JavaModelAnalysedInR`.
