# A flock of gliders flies through a small town (the gliders of gliders.R with a 3D scene).
#
# The town is built of 3D models and 3D shapes:
# - houses and trees (OBJ files with their materials, the roofs with a texture) and the church
#   tower (an STL file) are objects of the environment (m$load_model, m$place_model),
# - the office towers are boxes (m$draw_box), the streets and the square flat polygons
#   (m$draw_polygon3) with dashed centre lines (m$draw_path3),
# - a hot air balloon drifts over the town (m$draw_sphere, its basket a box, the ropes lines).
#
# The flock flies along a route: the waypoints are markers (m$draw_point3) joined by a line
# (m$draw_path3), a ring at the height of the next waypoint (m$draw_circle with z) shows where
# the flock goes, the labels stand above the buildings (m$draw_text with z). The boids keep the
# rules of C. Reynolds (separation, alignment, cohesion), steer to the next waypoint and fly
# around the buildings and the trees (only below their roofs: above them the way is free).
#
# The custom camera follows the red boid (in Bio7, the global view beside it). The charts: the
# order of the flock (polarization), the mean speed and the height of the flock. Export the
# model with "Export Simulation as HTML..." (the OBJ and STL models are converted to glTF).

m <- agent_model("GliderTown", "A flock of gliders flies along a route through a small town: houses, trees and a church tower (OBJ and STL), office towers, streets and a balloon (3D shapes).")

m$param("boids", 150, min = 20, max = 400, label = "Number of boids", restart = TRUE)
m$param("max_speed", 6.5, min = 1, max = 12, step = 0.1, label = "Maximum speed", group = "Flight", type = "double")
m$param("max_force", 0.05, min = 0.005, max = 0.2, step = 0.005, label = "Maximum force (turns)", group = "Flight")
m$param("route", 1, min = 0, max = 3, step = 0.1, label = "Follow the route", group = "Flight", type = "double")
m$param("separation", 1.5, min = 0, max = 3, step = 0.1, label = "Separation", group = "Rules", type = "double")
m$param("alignment", 1, min = 0, max = 3, step = 0.1, label = "Alignment", group = "Rules", type = "double")
m$param("cohesion", 1, min = 0, max = 3, step = 0.1, label = "Cohesion", group = "Rules", type = "double")
m$param("trees", 40, min = 0, max = 120, label = "Number of trees", group = "Town", restart = TRUE)
m$param("show_route", TRUE, label = "Show the route", group = "View")
m$param("labels", TRUE, label = "Labels", group = "View")
m$param("size", 6, min = 2, max = 15, step = 0.5, label = "Glider size", group = "View", type = "double")
m$param("follow", TRUE, label = "Camera follows the red boid", group = "View")

WIDTH <- 2000; TOP <- 760         # the box: 2000 x 2000, the height
C0 <- WIDTH / 2                   # the centre of the town
FLOOR_Z <- 70                     # the lowest height of the flight
RADIUS <- 150; CLOSE <- 45; AVOID <- 90
STREET <- 70                      # the width of the streets

# The route of the flock (x, y, z): along the main street, around the church, past the offices and the balloon
ROUTE <- matrix(c(320, 1000, 170, 780, 1000, 150, 1000, 680, 300, 1560, 700, 260, 1680, 1160, 430,
                  1420, 1640, 620, 900, 1560, 400, 430, 1430, 300, 330, 720, 230), ncol = 3, byrow = TRUE)

# The office towers (m$draw_box): the corner x, y, the width, the depth and the height
OFFICES <- matrix(c(1260, 1250, 130, 130, 460, 1450, 1270, 110, 110, 380, 1290, 1440, 120, 120, 540), ncol = 5, byrow = TRUE)

# The objects of the environment (they stay) and the obstacles of the flock (x, y, radius, height)
build_town <- function(m) {
  obstacles <- list()
  add <- function(x, y, r, h) obstacles[[length(obstacles) + 1]] <<- c(x, y, r, h)
  # the church tower in the middle of the square
  m$place_model("tower", C0, C0, size = 420)
  add(C0, C0, 60, 420)
  # the houses along the main street (west - east) and the cross street (south - north), the front to the street
  for (x in seq(300, 1749, by = 150)) {
    if (abs(x - C0) < 240) next
    for (side in c(-1, 1)) {
      size <- 100 + 40 * m$random(1)
      m$place_model("house", x, C0 + side * 125, heading = if (side < 0) 0 else 180, size = size)
      add(x, C0 + side * 125, 85, size)
    }
  }
  for (y in seq(300, 1749, by = 150)) {
    if (abs(y - C0) < 240) next
    for (side in c(-1, 1)) {
      if (side > 0 && y > C0) next          # the office quarter
      size <- 100 + 40 * m$random(1)
      m$place_model("house", C0 + side * 125, y, heading = if (side < 0) 90 else 270, size = size)
      add(C0 + side * 125, y, 85, size)
    }
  }
  for (k in seq_len(nrow(OFFICES))) {
    o <- OFFICES[k, ]
    add(o[1] + o[3] / 2, o[2] + o[4] / 2, max(o[3], o[4]) * 0.72, o[5])
  }
  # the trees in the gardens (away from the streets, the houses and the offices)
  planted <- 0
  for (i in seq_len(m$trees * 20)) {
    if (planted >= m$trees) break
    x <- m$random(1, 150, WIDTH - 150); y <- m$random(1, 150, WIDTH - 150)
    if (abs(x - C0) < 260 || abs(y - C0) < 260 || (x > 1180 && y > 1180)) next
    ob <- do.call(rbind, obstacles)
    if (any(sqrt((x - ob[, 1])^2 + (y - ob[, 2])^2) < ob[, 3] + 50)) next
    size <- 80 + 50 * m$random(1)
    m$place_model("tree", x, y, heading = m$random(1, 0, 360), size = size)
    add(x, y, 35, size)
    planted <- planted + 1
  }
  do.call(rbind, obstacles)
}

# The settings of the view and the shapes of the town (drawn again in each step)
view <- function(m) {
  m$agent_radius <- m$size
  m$camera_follow(if (isTRUE(m$follow)) 1 else NULL)
  # the streets and the square (flat polygons just above the floor), the dashed centre lines
  lo <- 120; hi <- WIDTH - 120; w <- STREET / 2
  m$draw_polygon3(c(lo, hi, hi, lo), c(C0 - w, C0 - w, C0 + w, C0 + w), rep(1, 4), "#6b7078")
  m$draw_polygon3(c(C0 - w, C0 + w, C0 + w, C0 - w), c(lo, lo, hi, hi), rep(1.2, 4), "#6b7078")
  a <- seq(0, 2 * pi, length.out = 9)[-9] + pi / 8
  m$draw_polygon3(C0 + 190 * cos(a), C0 + 190 * sin(a), rep(1.5, 8), "#c8bea6", "#8d846f")
  for (x0 in seq(lo, hi - 1, by = 80)) {
    if (abs(x0 - C0) > 200) {
      m$draw_path3(c(x0, x0 + 40), c(C0, C0), c(2, 2), "#f2f2f2", 2)
      m$draw_path3(c(C0, C0), c(x0, x0 + 40), c(2, 2), "#f2f2f2", 2)
    }
  }
  # the office towers
  for (k in seq_len(nrow(OFFICES))) {
    o <- OFFICES[k, ]
    m$draw_box(o[1], o[2], 0, o[3], o[4], o[5], "#8aa1bd", "#3d4a5c")
    m$draw_box(o[1] - 6, o[2] - 6, o[5], o[3] + 12, o[4] + 12, 12, "#56667c")
  }
  # the balloon: it drifts over the west of the town
  bx <- 560 + 160 * sin(m$t / 300); by <- 1480 + 60 * cos(m$t / 210); bz <- 470 + 25 * sin(m$t / 40)
  m$draw_sphere(bx, by, bz, 55, "#e0533c")
  m$draw_box(bx - 14, by - 14, bz - 105, 28, 28, 22, "#8b5a2b")
  for (d in list(c(-12, -12), c(12, -12), c(12, 12), c(-12, 12)))
    m$draw_line3(bx + d[1], by + d[2], bz - 83, bx + d[1] * 2.6, by + d[2] * 2.6, bz - 40, "#5d4037", 1)
  # the route: the markers, the line between them, a ring at the next waypoint
  target <- m$target
  if (isTRUE(m$show_route)) {
    loop <- rbind(ROUTE, ROUTE[1, ])
    m$draw_path3(loop[, 1], loop[, 2], loop[, 3], "#ffb300aa", 2)
    for (k in seq_len(nrow(ROUTE)))
      m$draw_point3(ROUTE[k, 1], ROUTE[k, 2], ROUTE[k, 3], if (k == target) 26 else 16, if (k == target) "#43a047" else "#fb8c00")
    m$draw_circle(ROUTE[target, 1], ROUTE[target, 2], 70, NULL, "#43a047", z = ROUTE[target, 3])
  }
  if (isTRUE(m$labels)) {
    m$draw_text(C0, C0, "Church", "#3e2723", 1, z = 450)
    m$draw_text(1320, 1500, "Offices", "#263238", 1, z = 580)
    m$draw_text(bx, by, "Balloon", "#b71c1c", 1, z = bz + 75)
    if (isTRUE(m$show_route))
      m$draw_text(ROUTE[target, 1], ROUTE[target, 2], paste("Waypoint", target), "#1b5e20", 1, z = ROUTE[target, 3] + 45)
  }
}

# A velocity of the maximum speed times f in a random direction (the climb: up to +- the angle)
random_velocity <- function(m, n, f, climb, a0 = 0, a1 = 2 * pi) {
  angle <- m$random(n, a0, a1); up <- m$random(n, -climb, climb)
  f * m$max_speed * cbind(cos(angle) * cos(up), sin(angle) * cos(up), sin(up))
}

m$init <- function(m) {
  m$box(WIDTH, WIDTH, TOP)
  m$agent_shape <- "glider"
  m$floor <- "plane"
  m$shadows <- TRUE
  m$load_model("house", "data/models/house.obj")
  m$load_model("tree", "data/models/tree.obj")
  m$load_model("tower", "data/models/tower.stl")
  m$obstacles <- build_town(m)
  n <- m$boids
  # the flock starts at the west end of the main street, flying east
  m$p <- cbind(180 + m$random(n, 0, 160), C0 + m$random(n, -150, 150), 200 + m$random(n, 0, 160))
  m$v <- random_velocity(m, n, 0.75, 0.1, -0.4, 0.4)
  colors <- hsv(m$random(n, 0.5, 0.62), 0.75, 0.95)
  colors[1] <- "#f2261f"
  m$agents <- data.frame(x = m$p[, 1], y = m$p[, 2], z = m$p[, 3], color = colors)
  m$target <- 1
  m$reached <- 0
  view(m)
}

m$step <- function(m) {
  p <- m$p; v <- m$v; o <- m$obstacles
  ms <- m$max_speed
  n <- nrow(p); k <- nrow(o)
  dx <- outer(p[, 1], p[, 1], function(i, j) j - i)
  dy <- outer(p[, 2], p[, 2], function(i, j) j - i)
  dz <- outer(p[, 3], p[, 3], function(i, j) j - i)
  d2 <- dx^2 + dy^2 + dz^2
  d2[d2 == 0] <- Inf
  near <- (d2 <= RADIUS^2) * 1
  count <- rowSums(near)
  cnt <- pmax(count, 1)
  close <- (d2 < CLOSE^2) / d2
  align <- (near %*% v) / cnt - v
  cohes <- cbind(rowSums(near * dx), rowSums(near * dy), rowSums(near * dz)) / cnt
  sep <- -cbind(rowSums(close * dx), rowSums(close * dy), rowSums(close * dz))
  steer <- (count > 0) * (m$alignment * 0.06 * align + m$cohesion * 0.0004 * cohes + m$separation * 2.5 * sep)
  # the route: to the next waypoint (the next one when the centre of the flock is near)
  goal <- ROUTE[m$target, ]
  to_goal <- sweep(-p, 2, goal, "+")
  steer <- steer + m$route * 0.08 * to_goal / pmax(sqrt(rowSums(to_goal^2)), 1e-9)
  if (sqrt(sum((colMeans(p) - goal)^2)) < 220) {
    m$target <- m$target %% nrow(ROUTE) + 1
    m$reached <- m$reached + 1
  }
  s <- sqrt(rowSums(steer^2))
  max_turn <- m$max_force * 12
  steer <- steer * ifelse(s > max_turn, max_turn / pmax(s, 1e-12), 1)
  # the buildings and the trees: a boid below the roof looks ahead and steers away (all obstacles at once)
  ax <- outer(p[, 1] + v[, 1] * 15, o[, 1], "-"); ay <- outer(p[, 2] + v[, 2] * 15, o[, 2], "-")
  nx <- outer(p[, 1], o[, 1], "-"); ny <- outer(p[, 2], o[, 2], "-")
  la <- sqrt(ax^2 + ay^2)
  gap <- pmin(la, sqrt(nx^2 + ny^2)) - matrix(o[, 3], n, k, byrow = TRUE)
  below <- outer(p[, 3], o[, 4] + 30, "<")
  push <- ifelse(below & gap < AVOID, ms * 0.25 * (1 - pmax(gap, 0) / AVOID), 0)
  la <- pmax(la, 1e-9)
  steer[, 1] <- steer[, 1] + rowSums(ax / la * push)
  steer[, 2] <- steer[, 2] + rowSums(ay / la * push)
  # up from a roof just below
  steer[, 3] <- steer[, 3] + (rowSums(below & gap < 0) > 0) * 0.05 * ms
  # the walls and the floor
  margin <- 120; wall <- 0.1 * ms
  steer[, 1] <- steer[, 1] - wall * (p[, 1] > WIDTH - margin) + wall * (p[, 1] < margin)
  steer[, 2] <- steer[, 2] - wall * (p[, 2] > WIDTH - margin) + wall * (p[, 2] < margin)
  steer[, 3] <- steer[, 3] - wall * (p[, 3] > TOP - margin) + wall * (p[, 3] < FLOOR_Z + 40)
  v <- v + steer
  speed <- sqrt(rowSums(v^2))
  v <- v * ifelse(speed > ms, ms / speed, ifelse(speed < ms / 2, ms / 2 / pmax(speed, 1e-9), 1))
  p <- p + v
  # never inside a building or a tree (pushed out to its side)
  nx <- outer(p[, 1], o[, 1], "-"); ny <- outer(p[, 2], o[, 2], "-")
  dist <- sqrt(nx^2 + ny^2)
  inside <- which(dist < matrix(o[, 3] + 2, n, k, byrow = TRUE) & outer(p[, 3], o[, 4], "<"), arr.ind = TRUE)
  for (r in seq_len(nrow(inside))) {
    i <- inside[r, 1]; j <- inside[r, 2]
    p[i, 1:2] <- o[j, 1:2] + c(nx[i, j], ny[i, j]) / max(dist[i, j], 1e-9) * (o[j, 3] + 2)
  }
  p[, 1:2] <- pmin(WIDTH, pmax(0, p[, 1:2]))
  p[, 3] <- pmin(TOP, pmax(FLOOR_Z * 0.5, p[, 3]))
  m$p <- p; m$v <- v
  m$agents$x <- p[, 1]; m$agents$y <- p[, 2]; m$agents$z <- p[, 3]
  unit <- v / pmax(sqrt(rowSums(v^2)), 1e-9)
  m$chart("Polarization", sqrt(sum(colMeans(unit)^2)))
  m$chart("Mean speed", mean(sqrt(rowSums(v^2))), chart = "Speed")
  m$chart("Mean height", mean(p[, 3]), chart = "Height of the flock")
  view(m)
}

m$action("scatter", function(m) {
  m$v <- random_velocity(m, nrow(m$p), 1, 0.6)
}, label = "Scatter the flock", group = "Flight")

simulation(m)
