# 3D plots (OpenGL) in the 3D view of Bio7 from R: layers with bio7_scatter3d(),
# bio7_surface3d(), bio7_bars3d(), bio7_line3d(), ... shown with bio7_plot3d().
# Rotate the plot with the mouse, zoom with the mouse wheel; the 3D view can be
# shown in fullscreen mode (F2, F3: second monitor).
#
#   bio7_plot3d(bio7_scatter3d(x, y, z), title = "...", xlab = "...", colorbar = "...")
#
# Each plot replaces the plot before: the script waits a few seconds between the
# plots (or run the sections one by one: select the lines and run the selection).

pause <- function(seconds = 4) Sys.sleep(seconds)

# 1. Scatter: the three species of the iris data as layers (legend), with lines
#    down to the floor
cols <- c(setosa = "#1F77B4", versicolor = "#FF7F0E", virginica = "#2CA02C")
layers <- lapply(names(cols), function(s) {
  d <- iris[iris$Species == s, ]
  bio7_scatter3d(d$Sepal.Length, d$Sepal.Width, d$Petal.Length, label = s,
                 color = cols[[s]], size = 0.12, drop_lines = TRUE)
})
bio7_plot3d(layers, title = "Iris", xlab = "Sepal length [cm]", ylab = "Sepal width [cm]",
            zlab = "Petal length [cm]")
pause()

# 2. Surface of a function with its contour lines on the floor: z[i, j] = f(x[i], y[j]),
#    a function or a matrix as outer(x, y, f)
x <- seq(-3, 3, length.out = 60)
y <- seq(-3, 3, length.out = 60)
f <- function(a, b) sin(a) * cos(b) * exp(-(a^2 + b^2) / 8)
bio7_plot3d(bio7_surface3d(x, y, f, colormap = "viridis", mesh = TRUE),
            bio7_contour3d(x, y, f, levels = 12, on_floor = TRUE, color = "gray"),
            title = "z = sin(x) cos(y) exp(-(x^2+y^2)/8)", colorbar = "z", legend = FALSE)
pause()

# 3. Bars: a 2D histogram of the eruptions of the Old Faithful geyser
#    (a table of counts as grid: rows = x bins, columns = y bins)
bx <- seq(1.5, 5.5, by = 0.5)
by <- seq(40, 100, by = 5)
counts <- table(cut(faithful$eruptions, bx), cut(faithful$waiting, by))
mid <- function(b) (head(b, -1) + tail(b, -1)) / 2
bio7_plot3d(bio7_bars3d(mid(bx), mid(by), unclass(counts), colormap = "Plasma", width = 0.8),
            title = "Old Faithful", xlab = "Eruption [min]", ylab = "Waiting [min]",
            zlab = "Count", colorbar = "Count", legend = FALSE)
pause()

# 4. A line: the Lorenz attractor (Euler steps)
lorenz <- function(n = 6000, dt = 0.006, s = 10, r = 28, b = 8 / 3) {
  m <- matrix(0, n, 3)
  m[1, ] <- c(1, 1, 1)
  for (i in 2:n) {
    p <- m[i - 1, ]
    m[i, ] <- p + dt * c(s * (p[2] - p[1]), p[1] * (r - p[3]) - p[2], p[1] * p[2] - b * p[3])
  }
  m
}
m <- lorenz()
bio7_plot3d(bio7_line3d(m, label = "Lorenz attractor", color = "#C2255C"),
            title = "Lorenz attractor", xlab = "x", ylab = "y", zlab = "z")
pause()

# 5. Points colored by a value (color bar): a noisy saddle, the values are the residuals
set.seed(1)
px <- runif(400, -2, 2); py <- runif(400, -2, 2)
pz <- px^2 - py^2 + rnorm(400, sd = 0.3)
bio7_plot3d(bio7_scatter3d(px, py, pz, values = pz - (px^2 - py^2), colormap = "diverging", size = 0.08),
            bio7_wireframe3d(seq(-2, 2, 0.25), seq(-2, 2, 0.25), function(a, b) a^2 - b^2,
                             color = "gray", label = "Model"),
            title = "Saddle and residuals", colorbar = "Residual")
pause()

# 6. A pie chart (shown alone)
bio7_plot3d(bio7_pie3d(table(mtcars$cyl), explode = 0.1), title = "Cars by cylinders")
pause()

# 7. Export: the plot as HTML page (WebGL, rotate it in any web browser), shown in
#    the browser view of Bio7
html <- file.path(tempdir(), "iris_3d.html")
bio7_plot3d(layers, title = "Iris (HTML)", xlab = "Sepal length", ylab = "Sepal width",
            zlab = "Petal length", html = html)
bio7_browse(html)

# bio7_plot3d_clear()   # removes the plot from the 3D view
