# Examples for Bio7

The examples are Eclipse projects, one for each language. Import them into Bio7 with
*File > Import > Existing Projects into Workspace* (select the folder of a clone or the
downloaded ZIP archive); you can import all of them or only the languages you need.

| Project | Contents |
|---|---|
| [Bio7_Java_Examples](Bio7_Java_Examples) | Java: the simulation API, models, ImageJ, R and Python from Java, 3D (OpenGL), plots, GUIs (SWT, WindowBuilder, Shiny and web apps), geodata (GDAL, WorldWind), flows |
| [Bio7_R_Examples](Bio7_R_Examples) | R: the R API of Bio7, simulations, snippets |
| [Bio7_Python_Examples](Bio7_Python_Examples) | Python: the Python API of Bio7 (image analysis, plots, R, jobs, 3D), simulations, OpenCV, markdown |
| [Bio7_Groovy_Examples](Bio7_Groovy_Examples) | Groovy: ImageJ, R, OpenGL, tables, the shell, image clustering |
| [Bio7_BeanShell_Examples](Bio7_BeanShell_Examples) | BeanShell: snippets, ImageJ, R, the Bio7 GUI, LibreOffice |
| [Bio7_ImageJ_Macro_Examples](Bio7_ImageJ_Macro_Examples) | ImageJ macros |
| [Bio7_JavaScript_Examples](Bio7_JavaScript_Examples) | JavaScript |
| [Bio7_Flow_Examples](Bio7_Flow_Examples) | Flows (the flow editor) and reusable flow components |
| [Bio7_Data](Bio7_Data) | Data for the 3D measurements (particles, a cave) |

The simulations of Java, R and Python read their images, 3D models and geodata from the folder
`data` of their project.

**Clone:** the flows store the workspace paths of their scripts (e.g.
`/Bio7_Flow_Examples/Examples/Snippet1/xdirection.bsh`), so keep the project names when you import
the projects.

**Earlier versions** had two projects, Bio7_Java_Examples and Bio7_Script_Examples. If you
imported them before, delete Bio7_Script_Examples from the workspace (its examples are now in the
language projects) and import the new projects. In Bio7_Java_Examples some packages were renamed
(e.g. `modelling` and `modeling3d` are now `models`, `webgui` is `gui.web`); see its README.
