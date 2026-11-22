# Sheep and wolves on a field (a continuous model) with images, shapes and several charts.
#
# The grass is a biomass (0 - 1, a matrix) which grows logistically, the animals (the rows of a data
# frame) move with a heading and a speed: the sheep prefer the directions with more grass, the wolves
# hunt the nearest sheep in their view. A fenced pasture (a polygon) protects the sheep.
#
# The drawing of each step: the animals as images rotated to their heading (m$draw_image), the hunting
# lines (m$draw_line), the view of the first wolf (m$draw_circle), the fence (m$draw_polygon) and a text
# (m$draw_text). Charts: the populations, the grass (a second chart), a phase plot (m$chart_xy) and the
# energies of the sheep (m$chart_bars). The images are in data/sprites of this project.

FENCE_X <- c(6, 22, 26, 10); FENCE_Y <- c(8, 5, 20, 24)

inside <- function(x, y) {
  # the point in the fenced pasture (a ray crossing test)
  result <- FALSE; n <- length(FENCE_X)
  for (i in seq_len(n)) {
    j <- if (i == 1) n else i - 1
    if ((FENCE_Y[i] > y) != (FENCE_Y[j] > y) &&
        x < (FENCE_X[j] - FENCE_X[i]) * (y - FENCE_Y[i]) / (FENCE_Y[j] - FENCE_Y[i]) + FENCE_X[i]) result <- !result
  }
  result
}

m <- value_grid_model("SheepWolves", "Sheep and wolves on a field: the sheep prefer the directions with more grass, the wolves hunt the nearest sheep in their view, a fenced pasture protects the sheep.")

m$param("sheep", 60, min = 10, max = 150, label = "Sheep at the start", restart = TRUE, group = "Start")
m$param("wolves", 8, min = 0, max = 30, label = "Wolves at the start", restart = TRUE, group = "Start")
m$param("growth", 0.04, min = 0, max = 0.2, step = 0.005, label = "Growth of the grass", group = "Field")
m$param("fence", TRUE, label = "Fenced pasture (a refuge)", group = "Field")
m$param("wolf_speed", 0.75, min = 0.2, max = 1.5, step = 0.05, label = "Speed of the wolves", group = "Animals")
m$param("view", 9, min = 2, max = 20, step = 0.5, label = "View of the wolves (cells)", group = "Animals", type = "double")

new_animals <- function(m, n, wolf) {
  data.frame(x = if (wolf) m$random(n, 30, m$width) else m$random(n, 0, m$width), y = m$random(n, 0, m$height),
             heading = m$random(n, 0, 360), energy = if (wolf) m$random(n, 10, 20) else m$random(n, 5, 10), wolf = rep(wolf, n), prey = rep(NA_integer_, n))
}

m$init <- function(m) {
  m$size(60, 45)
  m$range(0, 1)
  m$color_map("#b39b62", "#9ccc65", "#33691e")
  m$values <- 0.2 + 0.8 * outer(seq_len(m$height) - 1, seq_len(m$width) - 1, function(y, x) fractal_noise(x * 0.09, y * 0.09, 4))
  m$load_image("sheep", "data/sprites/sheep.png")
  m$load_image("wolf", "data/sprites/wolf.png")
  m$animals <- rbind(new_animals(m, m$sheep, FALSE), new_animals(m, m$wolves, TRUE))
  show_animals(m)
}

grass <- function(m, x, y) m$values[floor(y %% m$height) + 1, floor(x %% m$width) + 1]

m$step <- function(m) {
  v <- m$values
  m$values <- pmin(pmax(v + m$growth * v * (1 - v) + 0.001, 0), 1)       # the logistic growth (all cells)
  a <- m$animals
  born <- a[0, ]
  for (i in seq_len(nrow(a))) {
    if (a$energy[i] <= 0) next
    if (a$wolf[i]) {
      # a wolf: the nearest free sheep in the view, else a random walk
      free <- which(!a$wolf & a$energy > 0 & !(m$fence & mapply(inside, a$x, a$y)))
      d <- sqrt((a$x[free] - a$x[i])^2 + (a$y[free] - a$y[i])^2)
      a$prey[i] <- NA
      if (length(free) && min(d) < m$view) {
        k <- free[which.min(d)]
        a$prey[i] <- k
        a$heading[i] <- heading_of(a$x[k] - a$x[i], a$y[i] - a$y[k])
        if (min(d) < 0.8) { a$energy[k] <- 0; a$energy[i] <- a$energy[i] + 10; a$prey[i] <- NA }
      } else a$heading[i] <- (a$heading[i] + m$random(1, -30, 30)) %% 360
      ox <- a$x[i]; oy <- a$y[i]; speed <- if (is.na(a$prey[i])) m$wolf_speed * 0.6 else m$wolf_speed
      a$x[i] <- (a$x[i] + speed * sin_deg(a$heading[i])) %% m$width
      a$y[i] <- (a$y[i] - speed * cos_deg(a$heading[i])) %% m$height
      if (m$fence && inside(a$x[i], a$y[i])) { a$x[i] <- ox; a$y[i] <- oy; a$heading[i] <- (a$heading[i] + 180) %% 360 }
      a$energy[i] <- a$energy[i] - 0.25
      if (a$energy[i] > 30 && m$chance(0.04)) { a$energy[i] <- a$energy[i] / 2; born <- rbind(born, transform(a[i, ], heading = m$random(1, 0, 360), prey = NA)) }
    } else {
      # a sheep: three directions weighted by the grass ahead
      turns <- c(-40, 0, 40)
      w <- 0.05 + mapply(function(t) grass(m, a$x[i] + 2 * sin_deg(a$heading[i] + t), a$y[i] - 2 * cos_deg(a$heading[i] + t)), turns)
      a$heading[i] <- (a$heading[i] + turns[m$random_weighted(w)] + m$random(1, -10, 10)) %% 360
      a$x[i] <- (a$x[i] + 0.35 * sin_deg(a$heading[i])) %% m$width
      a$y[i] <- (a$y[i] - 0.35 * cos_deg(a$heading[i])) %% m$height
      cy <- floor(a$y[i]) + 1; cx <- floor(a$x[i]) + 1
      eaten <- min(0.3, m$values[cy, cx])
      m$values[cy, cx] <- m$values[cy, cx] - eaten
      a$energy[i] <- a$energy[i] + eaten * 3 - 0.45
      if (a$energy[i] > 12 && m$chance(0.06)) { a$energy[i] <- a$energy[i] / 2; born <- rbind(born, transform(a[i, ], heading = m$random(1, 0, 360))) }
    }
  }
  # the prey of the wolves as references to the sheep after the removal of the dead animals
  keep <- a$energy > 0
  newIndex <- cumsum(keep)
  a$prey <- ifelse(!is.na(a$prey) & keep[pmax(1, a$prey)], newIndex[pmax(1, a$prey)], NA)
  m$animals <- rbind(a[keep, ], born)
  show_animals(m)
}

show_animals <- function(m) {
  a <- m$animals
  if (m$fence) m$draw_polygon(FENCE_X, FENCE_Y, fill = "#ffffff22", stroke = "#f5f0e6")
  wolves <- which(a$wolf); sheep <- which(!a$wolf)
  for (i in wolves) if (!is.na(a$prey[i])) m$draw_line(a$x[i], a$y[i], a$x[a$prey[i]], a$y[a$prey[i]], "#d62728aa", 2)
  if (length(wolves)) m$draw_circle(a$x[wolves[1]], a$y[wolves[1]], m$view, NULL, "#ffffff88")
  for (i in seq_len(nrow(a))) m$draw_image(if (a$wolf[i]) "wolf" else "sheep", a$x[i], a$y[i], if (a$wolf[i]) 2.6 else 2.0, a$heading[i])
  m$draw_text(1, 2.5, sprintf("Step %d: %d sheep, %d wolves", m$t, length(sheep), length(wolves)), "#ffffff", 1.6)
  e <- a$energy[sheep]
  m$chart("Sheep", length(sheep))
  m$chart("Wolves", length(wolves))
  m$chart("Mean biomass", m$mean(), chart = "Grass")
  m$chart("Median energy of the sheep / 20", if (length(e)) median(e) / 20 else 0, chart = "Grass")
  m$chart_xy("Phase plot", "Sheep - wolves", length(sheep), length(wolves))
  bins <- tabulate(pmin(pmax(floor(e / 4), 0), 4) + 1, nbins = 5)
  m$chart_bars("Energies of the sheep", c("0-4", "4-8", "8-12", "12-16", "16+"), bins)
}

m$action("release_wolves", function(m) m$animals <- rbind(m$animals, new_animals(m, 3, TRUE)), label = "Release 3 wolves")

simulation(m)
