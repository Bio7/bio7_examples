# A heat diffusion: the heat flows to the cooler neighbors (a value grid with a color map).
#
# Run the script in Bio7 (ImageJ shows the values with the color map), start it with Play/Pause
# and export it with "Export Simulation as HTML..." (the page runs the script with webR).

m <- value_grid_model("HeatDiffusion", "A heat diffusion: the heat flows to the cooler neighbors, the plate loses heat at its surface. Add heat sources with the button.")

m$param("diffusion", 0.2, min = 0, max = 0.25, step = 0.005, label = "Diffusion")
m$param("cooling", 0.002, min = 0, max = 0.02, step = 0.001, label = "Cooling per step")
m$param("sources", 3, min = 1, max = 10, label = "Heat sources at the start", restart = TRUE)
m$param("torus", FALSE, label = "Torus (the heat flows over the borders)")

add_source <- function(m) {
  x <- m$random_int(11, m$width - 10); y <- m$random_int(11, m$height - 10)
  m$values[(y - 3):(y + 3), (x - 3):(x + 3)] <- 1
}

m$init <- function(m) {
  m$size(120, 120)
  m$range(0, 1)                                      # the values of the first and the last color
  m$color_map("#000004", "#51127c", "#b73779", "#fc8961", "#fcfdbf")
  for (k in seq_len(m$sources)) add_source(m)
}

m$step <- function(m) {
  m$wrap <- m$torus
  # the diffusion of all cells (matrices): laplacian() is the sum of the 4 neighbors - 4 * value
  m$values <- (m$values + m$diffusion * m$laplacian()) * (1 - m$cooling)
  m$chart("Mean temperature", m$mean())
  m$chart("Maximum temperature", max(m$values))
}

m$action("add_source", add_source, label = "Add a heat source")

simulation(m)
