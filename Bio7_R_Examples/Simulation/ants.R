# Ants collect food (agents on a grid).
#
# The grid is the pheromone (m$values: a value which diffuses and evaporates), the ants are
# agents on it (the rows of m$agents, drawn as arrows in their heading). A searching ant (red)
# follows the pheromone ahead of it and wiggles; when it finds a pile of food it takes a piece
# and turns home (green). On the way to the nest it lays pheromone, so the trails to the piles
# grow while the food lasts. The simulation stops when all food is in the nest (m$stop()).
#
# The agents: m$add_agents (with an own column carrying), m$forward and m$face (for many ants at
# once: the rows), m$agent_counts (the ants in each cell, a matrix as m$values). All ants of a
# step are handled with vectors (fast in R).
# Charts: the food in the nest, the food of the piles and the ants which carry food.

m <- value_grid_model("Ants", "Ants collect food: agents on a grid of pheromone which diffuses and evaporates.")

m$param("ants", 150, min = 10, max = 400, label = "Ants", restart = TRUE)
m$param("evaporation", 0.02, min = 0, max = 0.2, step = 0.005, label = "Evaporation of the pheromone", group = "Pheromone")
m$param("diffusion", 0.15, min = 0, max = 0.5, step = 0.01, label = "Diffusion of the pheromone", group = "Pheromone")
m$param("wiggle", 35, min = 0, max = 90, label = "Wiggle (degrees)", group = "Ants", type = "double")

SIZE <- 80
NEST_X <- 40; NEST_Y <- 40; NEST_R <- 4; PILE_R <- 5; FOOD <- 200
PILE_X <- c(14, 66, 58); PILE_Y <- c(16, 22, 68)

pile_at <- function(m, x, y) {
  for (k in 1:3) if (m$food[k] > 0 && sqrt((x - PILE_X[k])^2 + (y - PILE_Y[k])^2) < PILE_R) return(k)
  0
}

# The nest, the piles (smaller with less food) and the charts; the end when all food is in the nest
show_ants <- function(m) {
  m$draw_circle(NEST_X, NEST_Y, NEST_R, "#8d6e63cc", "#4e342e")
  carrying <- sum(m$agents$carrying)
  for (k in 1:3) {
    if (m$food[k] > 0) m$draw_circle(PILE_X[k], PILE_Y[k], PILE_R * sqrt(m$food[k] / FOOD), "#ffca28cc", "#ff8f00")
    m$chart(paste("Pile", k), m$food[k], chart = "Food of the piles")
  }
  m$chart("Food in the nest", m$delivered)
  m$chart("Ants carrying food", carrying)
  if (sum(m$food) == 0 && carrying == 0) m$stop()
}

m$init <- function(m) {
  m$size(SIZE, SIZE)
  m$range(0, 30)
  m$color_map("#1b1b1b", "#3b528b", "#21908c", "#5dc963", "#fde725")
  m$food <- c(FOOD, FOOD, FOOD)
  m$delivered <- 0
  m$add_agents(m$ants, x = NEST_X, y = NEST_Y, heading = m$random(m$ants, 0, 360), color = "#e53935", size = 1.8, shape = "arrow", carrying = FALSE)
  show_ants(m)
}

m$step <- function(m) {
  # the pheromone diffuses and evaporates (all cells at once)
  m$values <- (m$values + m$diffusion * m$laplacian() / 4) * (1 - m$evaporation)
  a <- m$agents
  n <- nrow(a)
  # the ants with food at the nest deliver it; searching ants at a pile take a piece (one after the other: a pile can run out)
  home <- a$carrying & sqrt((a$x - NEST_X)^2 + (a$y - NEST_Y)^2) < NEST_R
  pick <- rep(FALSE, n)
  for (i in which(!a$carrying)) {
    k <- pile_at(m, a$x[i], a$y[i])
    if (k > 0) { m$food[k] <- m$food[k] - 1; pick[i] <- TRUE }
  }
  turn <- home | pick
  m$agents$heading[turn] <- (a$heading[turn] + 180) %% 360
  m$agents$carrying[home] <- FALSE
  m$agents$carrying[pick] <- TRUE
  m$agents$color <- ifelse(m$agents$carrying, "#43a047", "#e53935")
  m$delivered <- m$delivered + sum(home)
  # the ants on the way home lay pheromone (each ant 25 in its cell) and turn to the nest
  back <- which(a$carrying & !home)
  if (length(back)) {
    m$values[] <- pmin(100, m$values + 25 * m$agent_counts(back))
    m$face(back, NEST_X, NEST_Y)
    m$agents$heading[back] <- m$agents$heading[back] + m$random(length(back), -m$wiggle / 3, m$wiggle / 3)
  }
  # the searching ants follow the pheromone two cells ahead (left, ahead, right) and wiggle
  search <- which(!a$carrying & !pick)
  if (length(search)) {
    h <- a$heading[search]
    turns <- c(-45, 0, 45)
    v <- vapply(turns, function(t) {
      cx <- floor(a$x[search] + 2 * sin_deg(h + t)) %% SIZE + 1
      cy <- floor(a$y[search] - 2 * cos_deg(h + t)) %% SIZE + 1
      m$values[cbind(cy, cx)]
    }, numeric(length(search)))
    v <- matrix(v, ncol = 3)
    best <- max.col(v, ties.method = "first")
    follow <- v[cbind(seq_along(search), best)] > 0.05
    m$agents$heading[search] <- h + ifelse(follow, turns[best], 0) + m$random(length(search), -m$wiggle, m$wiggle)
  }
  m$forward(c(back, search), 1)
  show_ants(m)
}

m$action("new_food", function(m) {
  m$food <- c(FOOD, FOOD, FOOD)
  show_ants(m)
}, label = "New food")

simulation(m)
