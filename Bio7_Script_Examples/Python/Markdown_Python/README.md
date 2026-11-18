# Python chunks in the markdown editor

Open a `.Rmd` (or `.md`) file in the markdown editor of Bio7. A chunk which starts with
```` ```{python} ```` runs in the embedded Python of Bio7 (started automatically), a chunk
with ```` ```{r} ```` in R. Use the buttons of the bar above a chunk: ▶ run the chunk,
▶▶ run the chunks above, ✕ remove the output. *Run All* runs all chunks of the document.

The output (printed text, the value of the last expression, errors) and the matplotlib
figures are shown below the chunk. The variables are created in `__main__` (also visible
in the Python console and the Python-Shell view).

| File | Content |
|------|---------|
| 01_Python_Chunks_Basics.Rmd | numpy, pandas, a regression, plots, errors |
| 02_Python_and_R_Chunks.Rmd | Python and R chunks in one document, data exchanged with `bio7.from_r`, `bio7.r`, `bio7.to_r` (R must run) |
| 03_ImageJ_Image_Analysis.Rmd | An ImageJ image in Python (`bio7.image`), segmentation, measurements, results to ImageJ and R |
| 04_Data_Analysis_Report.Rmd | A logistic model fitted to data, a stochastic simulation, a small report |
| 05_Interactive_Charts.Rmd | Interactive charts: plotly, altair, bokeh, folium, hvplot (Browser view inline, embedded when rendered) |
| 06_Statistical_Plots.Rmd | seaborn, plotnine (grammar of graphics), pandas and statsmodels plots |

Interactive charts (plotly, altair, bokeh, folium, holoviews/hvplot) are shown in the
Browser view of Bio7 when a chunk runs in the editor, and embedded in the document when
it is rendered as HTML (`chart.show()`, `bio7.show_html(chart)` or the chart as the last
expression of a chunk).

Note: the inline output uses the embedded Python of Bio7. When the document is rendered
with rmarkdown (knitr) in the embedded R of Bio7, the Python chunks also run in the
embedded Python (no reticulate needed; the chunk option `engine.bio7=FALSE` uses reticulate).
