# Dynamic 3D plots (OpenGL) in the 3D view of Bio7 from R: a plot updated in each
# step (bio7_plot3d(..., plot = p): the camera and the zoom are kept), a recorded
# animation (bio7_plot3d_recorder(): an HTML page with Play/Pause and a timeline)
# and the charts (histogram, line graph, area, box plot, violin, radar, rose, treemap).
#
#   p <- bio7_plot3d(bio7_histogram3d(x), zlim = c(0, 100))   # shown once
#   for (step in 1:100) bio7_plot3d(bio7_histogram3d(x), plot = p)   # updated
#
# Fixed ranges (xlim, ylim, zlim) keep the axes steady while the data changes.

pause <- function(seconds = 3) Sys.sleep(seconds)

# 1. An evolving trait: the body size in two populations with different optima
#    (selection and mutation), the histograms updated in each generation
set.seed(3)
n <- 800
trait <- list(Island = rnorm(n, 50, 5), Mainland = rnorm(n, 50, 5))
optimum <- c(Island = 35, Mainland = 65)
generation <- function(x, opt) {
  a <- sample(x, length(x), TRUE); b <- sample(x, length(x), TRUE)
  parent <- ifelse(abs(a - opt) < abs(b - opt), a, b)
  pmin(100, pmax(0, parent + rnorm(length(x), 0, 1.5)))
}
p <- bio7_plot3d(bio7_histogram3d(trait, bin_width = 2.5), title = "Evolution of a trait\nGeneration 0",
                 xlab = "Body size", ylab = "Population", zlab = "Individuals",
                 xlim = c(0, 100), zlim = c(0, 300))
for (g in 1:120) {
  trait <- Map(generation, trait, optimum)
  bio7_plot3d(bio7_histogram3d(trait, bin_width = 2.5), title = paste0("Evolution of a trait\nGeneration ", g), plot = p)
  Sys.sleep(0.03)
}
pause()

# 2. Growing line graphs: logistic growth of three populations (the series grow in
#    each step, each series at its own depth)
r <- c(Fast = 0.25, Medium = 0.15, Slow = 0.08)
K <- 100
pop <- matrix(5, 1, 3, dimnames = list(NULL, names(r)))
p <- bio7_plot3d(bio7_linegraph3d(0, pop), title = "Logistic growth", xlab = "Time", zlab = "Individuals",
                 xlim = c(0, 60), zlim = c(0, 110))
for (t in 1:60) {
  last <- pop[nrow(pop), ]
  pop <- rbind(pop, last + r * last * (1 - last / K))
  bio7_plot3d(bio7_linegraph3d(0:t, pop, fill = TRUE, marker_size = 0.08), plot = p)
  Sys.sleep(0.05)
}
pause()

# 3. Recorded: random walkers of two species drifting to two centers, recorded as an
#    animation (without the 3D view) and opened as HTML page in the browser view
set.seed(1)
walkers <- lapply(1:2, function(s) matrix(runif(150 * 3, 0, 100), ncol = 3))
centers <- list(c(30, 30, 60), c(70, 70, 40))
rec <- bio7_plot3d_recorder(speed = 20)
for (t in 1:120) {
  for (s in 1:2) {
    w <- walkers[[s]]
    w <- w + matrix(rnorm(length(w), 0, 2), ncol = 3) + sweep(-w, 2, centers[[s]], "+") * 0.03
    w[] <- pmin(100, pmax(0, w))
    walkers[[s]] <- w
  }
  bio7_plot3d(bio7_scatter3d(walkers[[1]], label = "Species A", size = 0.12),
              bio7_scatter3d(walkers[[2]], label = "Species B", size = 0.12),
              title = paste0("Random walkers\nStep ", t), xlab = "x [m]", ylab = "y [m]", zlab = "z [m]",
              xlim = c(0, 100), ylim = c(0, 100), zlim = c(0, 100), record = rec, show = FALSE)
}
bio7_plot3d_export(rec, file.path(tempdir(), "random_walkers.html"))
pause(5)

# 4. The other charts
bio7_plot3d(bio7_boxplot3d(iris[, 1:4]), title = "Iris: box plots", zlab = "cm")
pause()
bio7_plot3d(bio7_violin3d(split(iris$Petal.Length, iris$Species)), title = "Petal length: violins", zlab = "cm")
pause()
bio7_plot3d(bio7_area3d(1:20, cbind(Grass = 20 + 1:20, Shrubs = 10 + sqrt(1:20) * 3, Trees = (1:20)^1.3)),
            title = "Land cover (stacked areas)", xlab = "Year", zlab = "Area [ha]")
pause()
means <- aggregate(iris[, 1:4], list(Species = iris$Species), mean)
rownames(means) <- means$Species
bio7_plot3d(bio7_radar3d(means[, -1], rings = 4), title = "Iris: the mean of the species")
pause()
wind <- matrix(c(4, 6, 3, 2, 5, 8, 6, 3,  2, 3, 2, 1, 3, 5, 4, 2,  1, 1, 1, 0, 1, 2, 2, 1), 8, 3,
               dimnames = list(c("N", "NE", "E", "SE", "S", "SW", "W", "NW"), c("< 3 m/s", "3-8 m/s", "> 8 m/s")))
bio7_plot3d(bio7_rose3d(wind), title = "Wind rose")
pause()
bio7_plot3d(bio7_treemap3d(c(Forest = 45, Grassland = 30, Water = 15, Urban = 10), heights = TRUE),
            title = "Land cover (treemap)")
