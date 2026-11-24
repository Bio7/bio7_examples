# The Game of Life of J. H. Conway: a dead cell with 3 living neighbors is born, a living cell
# with 2 or 3 living neighbors survives, all other cells die or stay dead.
#
# Run the script in Bio7 (the grid opens in ImageJ with the settings on the left), start it with
# the Play/Pause button and export it with "Export Simulation as HTML..." (the same script runs
# in the web browser with webR).

m <- grid_model("GameOfLife", "The Game of Life of J. H. Conway: a dead cell with 3 living neighbors is born, a living cell with 2 or 3 living neighbors survives, all other cells die or stay dead.")

m$param("grid_size", 120, min = 20, max = 300, label = "Grid size (cells)", restart = TRUE)
m$param("density", 0.3, min = 0, max = 1, step = 0.01, label = "Start density", restart = TRUE)
m$param("torus", TRUE, label = "Torus world (wrap at the borders)")

m$init <- function(m) {
  m$size(m$grid_size, m$grid_size)           # a new grid (a matrix), all states 0
  m$palette("#ffffff", "#1f3a93")             # the colors of the states 0 and 1
  m$cells[] <- as.integer(m$random(length(m$cells)) < m$density)
}

m$step <- function(m) {
  m$wrap <- m$torus
  n <- m$count_neighbors(1)                   # the living neighbors of all cells (a matrix)
  alive <- m$cells == 1
  m$cells[] <- as.integer(n == 3 | (alive & n == 2))
  m$chart("Living cells", m$count(1))
}

m$action("glider", function(m) {
  x <- m$width %/% 2; y <- m$height %/% 2
  for (d in list(c(1, 0), c(2, 1), c(0, 2), c(1, 2), c(2, 2))) m$set(x + d[1], y + d[2], 1)
}, label = "Add a glider")

simulation(m)
