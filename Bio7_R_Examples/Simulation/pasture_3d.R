# Sheep and wolves on a pasture in 3D with animated glTF models.
#
# The sheep graze (the animation "graze"), walk and run away from the wolves ("run"); the wolves walk
# or hunt the nearest sheep outside the fenced pasture. A caught sheep returns into the fence. The
# models are in data/models of this project (m$load_model; the columns model, animation, size and
# heading of m$agents). The drawing: the fence on the floor (m$draw_polygon) and the hunting lines in 3D
# (m$draw_line3). The page shows the models with WebGL.

FENCE_X <- c(6, 26, 30, 8); FENCE_Y <- c(6, 4, 24, 28)

inside <- function(x, y) {
  result <- FALSE; n <- length(FENCE_X)
  for (i in seq_len(n)) {
    j <- if (i == 1) n else i - 1
    if ((FENCE_Y[i] > y) != (FENCE_Y[j] > y) &&
        x < (FENCE_X[j] - FENCE_X[i]) * (y - FENCE_Y[i]) / (FENCE_Y[j] - FENCE_Y[i]) + FENCE_X[i]) result <- !result
  }
  result
}

m <- agent_model("Pasture3D", "Sheep and wolves on a pasture in 3D with animated glTF models: the sheep graze, walk and run away from the wolves, the wolves hunt the sheep outside the fence.")

m$param("sheep", 30, min = 5, max = 80, label = "Sheep", restart = TRUE)
m$param("wolves", 3, min = 0, max = 10, label = "Wolves", restart = TRUE)
m$param("wolf_speed", 0.42, min = 0.1, max = 1, step = 0.05, label = "Speed of the wolves")
m$param("flight", 9, min = 2, max = 20, step = 0.5, label = "Distance of the flight (sheep)", type = "double")

m$init <- function(m) {
  m$box(60, 60, 12)
  m$load_model("sheep", "data/models/sheep.glb")
  m$load_model("wolf", "data/models/wolf.glb")
  m$catches <- 0
  ns <- m$sheep; nw <- m$wolves
  out <- seq_len(ns) %% 3 == 1                         # a third on the open pasture, two thirds in the fence
  sx <- ifelse(out, m$random(ns, 32, 56), m$random(ns, 8, 26)); sy <- ifelse(out, m$random(ns, 4, 56), m$random(ns, 8, 24))
  m$agents <- data.frame(x = c(sx, m$random(nw, 40, 58)), y = c(sy, m$random(nw, 30, 58)), z = 0,
                         wolf = c(rep(FALSE, ns), rep(TRUE, nw)), model = c(rep("sheep", ns), rep("wolf", nw)),
                         size = c(rep(2.2, ns), rep(2.8, nw)), heading = m$random(ns + nw, 0, 360),
                         animation = c(rep("graze", ns), rep("walk", nw)), rest = 0, prey = NA_integer_)
  draw(m)
}

m$step <- function(m) {
  a <- m$agents
  running <- 0; distances <- 0; hunting <- 0
  sheep <- which(!a$wolf); wolves <- which(a$wolf)
  for (i in seq_len(nrow(a))) {
    if (a$wolf[i]) {
      free <- sheep[!mapply(inside, a$x[sheep], a$y[sheep])]
      d <- sqrt((a$x[free] - a$x[i])^2 + (a$y[free] - a$y[i])^2)
      speed <- 0.15; a$animation[i] <- "walk"; a$prey[i] <- NA
      if (length(free) && min(d) < 25) {
        k <- free[which.min(d)]
        a$prey[i] <- k
        a$heading[i] <- heading_of(a$x[k] - a$x[i], a$y[k] - a$y[i])
        speed <- m$wolf_speed; a$animation[i] <- "run"
        distances <- distances + min(d); hunting <- hunting + 1
        if (min(d) < 1.2) { a$x[k] <- m$random(1, 10, 24); a$y[k] <- m$random(1, 10, 22); m$catches <- m$catches + 1 }
      } else a$heading[i] <- (a$heading[i] + m$random(1, -25, 25)) %% 360
      ox <- a$x[i]; oy <- a$y[i]
      a$x[i] <- a$x[i] + speed * sin_deg(a$heading[i]); a$y[i] <- a$y[i] + speed * cos_deg(a$heading[i])
      if (inside(a$x[i], a$y[i])) { a$x[i] <- ox; a$y[i] <- oy; a$heading[i] <- (a$heading[i] + 180) %% 360 }
    } else {
      dw <- sqrt((a$x[wolves] - a$x[i])^2 + (a$y[wolves] - a$y[i])^2)
      if (length(wolves) && min(dw) < m$flight) {
        w <- wolves[which.min(dw)]
        a$heading[i] <- heading_of(a$x[i] - a$x[w], a$y[i] - a$y[w])
        a$animation[i] <- "run"; a$rest[i] <- 0
        a$x[i] <- a$x[i] + 0.4 * sin_deg(a$heading[i]); a$y[i] <- a$y[i] + 0.4 * cos_deg(a$heading[i])
        running <- running + 1
      } else if (a$rest[i] > 0) {
        a$animation[i] <- "graze"; a$rest[i] <- a$rest[i] - 1
      } else {
        a$animation[i] <- "walk"
        a$heading[i] <- (a$heading[i] + m$random(1, -20, 20)) %% 360
        a$x[i] <- a$x[i] + 0.1 * sin_deg(a$heading[i]); a$y[i] <- a$y[i] + 0.1 * cos_deg(a$heading[i])
        if (m$chance(0.08)) a$rest[i] <- m$random_int(5, 24)
      }
    }
  }
  m$agents <- a
  m$keep_in_box()
  draw(m)
  m$chart("Sheep running", running)
  m$chart("Catches", m$catches)
  m$chart("Mean distance wolf - prey", if (hunting) distances / hunting else 0, chart = "Hunt")
}

draw <- function(m) {
  a <- m$agents
  m$draw_polygon(FENCE_X, FENCE_Y, fill = "#7cb34266", stroke = "#f5f0e6")
  for (i in which(a$wolf & !is.na(a$prey))) m$draw_line3(a$x[i], a$y[i], 1, a$x[a$prey[i]], a$y[a$prey[i]], 1, "#d62728", 2)
}

m$action("scatter", function(m) {
  s <- !m$agents$wolf
  m$agents$x[s] <- m$random(sum(s), 30, 58); m$agents$y[s] <- m$random(sum(s), 2, 58)
}, label = "Open the fence: all sheep out")

simulation(m)
