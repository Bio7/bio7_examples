# A flock of gliders in 3D (as the Java example modeling3d/swarm/BoidSphereModel).
#
# Each boid is a glider of triangles (m$agent_shape <- "glider"): it points in its direction
# of flight, climbs, dives and banks into turns. The flock follows the rules of C. Reynolds
# within the neighbourhood of a boid - separation (away from close boids), alignment (the
# mean direction) and cohesion (to their centre); the boids turn back softly at the walls of
# the flight space above the floor, fly around pillars (obstacles) and fly between half and
# the full maximum speed. All boids decide first, then all move (one time step), computed
# for all boids at once with matrices.
#
# The 3D view shows a dark floor with soft shadows (m$floor <- "plane", m$shadows <- TRUE),
# the custom camera follows the red boid (m$camera_follow(1), the global view beside it in
# the split view). The charts: the polarization of the flock (0: disordered, 1: all fly in
# parallel) and the mean speed. Run the script in Bio7, export it with "Export Simulation as
# HTML..." (WebGL in the page: the gliders, the pillars, the floor and the shadows; the orbit view).

m <- agent_model("Gliders", "A flock of gliders in 3D: separation, alignment and cohesion, pillars as obstacles, a camera which follows a boid.")

m$param("boids", 200, min = 20, max = 500, label = "Number of boids", restart = TRUE)
m$param("max_speed", 6.2, min = 1, max = 12, step = 0.1, label = "Maximum speed", group = "Flight", type = "double")
m$param("max_force", 0.05, min = 0.005, max = 0.2, step = 0.005, label = "Maximum force (turns)", group = "Flight")
m$param("separation", 1.5, min = 0, max = 3, step = 0.1, label = "Separation", group = "Rules", type = "double")
m$param("alignment", 1, min = 0, max = 3, step = 0.1, label = "Alignment", group = "Rules", type = "double")
m$param("cohesion", 1, min = 0, max = 3, step = 0.1, label = "Cohesion", group = "Rules", type = "double")
m$param("obstacles", TRUE, label = "Pillars", group = "Obstacles", restart = TRUE)
m$param("pillars", 8, min = 1, max = 30, label = "Number of pillars", group = "Obstacles", restart = TRUE)
m$param("placement", "Random", options = c("Random", "Fixed (centre)"), label = "Placement", group = "Obstacles", restart = TRUE)
m$param("size", 5, min = 2, max = 15, step = 0.5, label = "Glider size", group = "View", type = "double")
m$param("floor_style", "plane", options = c("plane", "grid", "none"), label = "Floor", group = "View")
m$param("show_shadows", TRUE, label = "Shadows", group = "View")
m$param("follow", TRUE, label = "Camera follows the red boid", group = "View")

# The flight space in the box (as BoidSphereModel: a floor of 2000 x 2000)
WIDTH <- 2000; FLOOR_Z <- 120; TOP <- 720
RADIUS <- 150; CLOSE <- 45; AVOID <- 110    # the neighbourhood, close boids, the distance to avoid a pillar
HALF <- WIDTH / 2 * 0.9                     # x and y within the centre +- HALF

# The pillars (a matrix: x, y, radius): random (a little apart) or fixed (the centre, then rings)
make_pillars <- function(m) {
  n <- if (isTRUE(m$obstacles)) m$pillars else 0
  o <- matrix(numeric(), 0, 3)
  c0 <- WIDTH / 2
  for (i in seq_len(n) - 1) {
    if (startsWith(m$placement, "Random")) {
      r <- 30 + m$random() * 50
      for (try in 1:30) {
        x <- c0 + m$random(1, -0.8, 0.8) * HALF; y <- c0 + m$random(1, -0.8, 0.8) * HALF
        if (nrow(o) == 0 || all(sqrt((x - o[, 1])^2 + (y - o[, 2])^2) >= r + o[, 3] + 120)) break
      }
      o <- rbind(o, c(x, y, r))
    } else if (i == 0) {
      o <- rbind(o, c(c0, c0, 45))
    } else {
      ring <- 1; first <- 1
      while (i >= first + 6 * ring) { first <- first + 6 * ring; ring <- ring + 1 }
      rings <- 0; k <- 1
      while (k < n) { rings <- rings + 1; k <- k + 6 * rings }
      dist <- ring * min(220, HALF * 0.85 / max(1, rings))
      a <- 2 * pi * (i - first) / (6 * ring) + ring * 0.3
      o <- rbind(o, c(c0 + dist * cos(a), c0 + dist * sin(a), 45))
    }
  }
  o
}

# The settings of the view (changed while the model runs) and the pillars (drawn in each step)
view <- function(m) {
  m$agent_radius <- m$size
  m$floor <- m$floor_style
  m$shadows <- isTRUE(m$show_shadows)
  m$camera_follow(if (isTRUE(m$follow)) 1 else NULL)
  for (k in seq_len(nrow(m$columns))) m$draw_pillar(m$columns[k, 1], m$columns[k, 2], m$columns[k, 3], TOP * 0.95)
}

# A velocity of the maximum speed times f in a random direction (the climb: up to +- the angle)
random_velocity <- function(m, n, f, climb) {
  angle <- m$random(n, 0, 2 * pi); up <- m$random(n, -climb, climb)
  f * m$max_speed * cbind(cos(angle) * cos(up), sin(angle) * cos(up), sin(up))
}

m$init <- function(m) {
  m$box(WIDTH, WIDTH, TOP + 40)
  m$agent_shape <- "glider"
  n <- m$boids
  c0 <- WIDTH / 2
  # in the flight space, flying in random directions at three quarters of the maximum speed
  m$p <- cbind(c0 + m$random(n, -0.65, 0.65) * HALF, c0 + m$random(n, -0.65, 0.65) * HALF,
               FLOOR_Z + (0.2 + 0.6 * m$random(n)) * (TOP - FLOOR_Z))
  m$v <- random_velocity(m, n, 0.75, 0.3)
  # each boid a shade of blue-cyan, the first one (the camera) red
  colors <- hsv(m$random(n, 0.5, 0.62), 0.75, 0.95)
  colors[1] <- "#f2261f"
  m$agents <- data.frame(x = m$p[, 1], y = m$p[, 2], z = m$p[, 3], color = colors)
  m$columns <- make_pillars(m)
  view(m)
}

m$step <- function(m) {
  p <- m$p; v <- m$v
  ms <- m$max_speed
  # the other boids within the neighbourhood: dx[i, j] the vector from boid i to boid j
  dx <- outer(p[, 1], p[, 1], function(i, j) j - i)
  dy <- outer(p[, 2], p[, 2], function(i, j) j - i)
  dz <- outer(p[, 3], p[, 3], function(i, j) j - i)
  d2 <- dx^2 + dy^2 + dz^2
  d2[d2 == 0] <- Inf                          # itself (and a boid at the same place): not a neighbour
  near <- (d2 <= RADIUS^2) * 1
  count <- rowSums(near)
  has <- count > 0
  cnt <- pmax(count, 1)
  close <- (d2 < CLOSE^2) / d2                # separation: the closer, the stronger (1 / distance)
  align <- (near %*% v) / cnt - v
  cohes <- cbind(rowSums(near * dx), rowSums(near * dy), rowSums(near * dz)) / cnt
  sep <- -cbind(rowSums(close * dx), rowSums(close * dy), rowSums(close * dz))
  steer <- has * (m$alignment * 0.06 * align + m$cohesion * 0.0004 * cohes + m$separation * 2.5 * sep)
  # the steering limited by the maximum force
  s <- sqrt(rowSums(steer^2))
  max_turn <- m$max_force * 12
  steer <- steer * ifelse(s > max_turn, max_turn / pmax(s, 1e-12), 1)
  # the pillars: a boid looks ahead (15 steps), the closer its path to a pillar the stronger it steers away
  for (k in seq_len(nrow(m$columns))) {
    o <- m$columns[k, ]
    fx <- p[, 1] + v[, 1] * 15 - o[1]; fy <- p[, 2] + v[, 2] * 15 - o[2]
    gap <- pmin(sqrt(fx^2 + fy^2), sqrt((p[, 1] - o[1])^2 + (p[, 2] - o[2])^2)) - o[3]
    push <- ifelse(gap < AVOID, ms * 0.25 * (1 - pmax(gap, 0) / AVOID), 0)
    len <- pmax(sqrt(fx^2 + fy^2), 1e-9)
    steer[, 1] <- steer[, 1] + fx / len * push
    steer[, 2] <- steer[, 2] + fy / len * push
  }
  # the walls: turn back softly near the edges of the flight space
  c0 <- WIDTH / 2; margin <- min(120, HALF * 0.2); wall <- 0.1 * ms
  steer[, 1] <- steer[, 1] - wall * (p[, 1] > c0 + HALF - margin) + wall * (p[, 1] < c0 - HALF + margin)
  steer[, 2] <- steer[, 2] - wall * (p[, 2] > c0 + HALF - margin) + wall * (p[, 2] < c0 - HALF + margin)
  steer[, 3] <- steer[, 3] - wall * (p[, 3] > TOP - margin) + wall * (p[, 3] < FLOOR_Z + margin)
  v <- v + steer
  # the speed between a half and the whole maximum
  speed <- sqrt(rowSums(v^2))
  v <- v * ifelse(speed > ms, ms / speed, ifelse(speed < ms / 2, ms / 2 / pmax(speed, 1e-9), 1))
  # all move; never inside a pillar (pushed out to its surface), not far outside the flight space
  p <- p + v
  for (k in seq_len(nrow(m$columns))) {
    o <- m$columns[k, ]
    nx <- p[, 1] - o[1]; ny <- p[, 2] - o[2]; dist <- sqrt(nx^2 + ny^2)
    inside <- dist < o[3] + 2
    p[inside, 1] <- o[1] + nx[inside] / pmax(dist[inside], 1e-9) * (o[3] + 2)
    p[inside, 2] <- o[2] + ny[inside] / pmax(dist[inside], 1e-9) * (o[3] + 2)
  }
  out <- min(200, HALF * 0.1)
  p[, 1:2] <- pmin(c0 + HALF + out, pmax(c0 - HALF - out, p[, 1:2]))
  p[, 3] <- pmin(TOP + out, pmax(FLOOR_Z * 0.5, p[, 3]))
  m$p <- p; m$v <- v
  m$agents$x <- p[, 1]; m$agents$y <- p[, 2]; m$agents$z <- p[, 3]
  # the charts: the order of the flock and the mean speed
  unit <- v / pmax(sqrt(rowSums(v^2)), 1e-9)
  m$chart("Polarization", sqrt(sum(colMeans(unit)^2)))
  m$chart("Mean speed", mean(sqrt(rowSums(v^2))), chart = "Speed")
  view(m)
}

m$action("scatter", function(m) {
  m$v <- random_velocity(m, nrow(m$p), 1, 0.6)
}, label = "Scatter the flock", group = "Flight")

m$action("new_pillars", function(m) {
  m$columns <- make_pillars(m)
  view(m)
}, label = "Place the pillars again", group = "Obstacles")

simulation(m)
