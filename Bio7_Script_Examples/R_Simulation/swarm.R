# A swarm of boids in 3D (separation, alignment and cohesion of C. Reynolds).
#
# The agents are the rows of the data frame m$agents (x, y, z, color and the velocity vx, vy, vz);
# the movement is computed for all boids at once with matrices. Run the script in Bio7 (the 3D
# view with Options3D and the settings on the left), export it as HTML (WebGL in the page).

m <- agent_model("Swarm", "A swarm of boids in 3D: each boid keeps a distance to its neighbors (separation), flies in their direction (alignment) and to their center (cohesion).")

m$param("boids", 150, min = 10, max = 400, label = "Number of boids", restart = TRUE)
m$param("view", 12, min = 2, max = 30, step = 0.5, label = "View radius", group = "Rules", type = "double")
m$param("separation", 0.04, min = 0, max = 0.2, step = 0.005, label = "Separation", group = "Rules")
m$param("alignment", 0.06, min = 0, max = 0.2, step = 0.005, label = "Alignment", group = "Rules")
m$param("cohesion", 0.006, min = 0, max = 0.05, step = 0.001, label = "Cohesion", group = "Rules")
m$param("max_speed", 1.5, min = 0.2, max = 4, step = 0.1, label = "Maximum speed", group = "Rules")

m$init <- function(m) {
  m$box(100, 100, 60)                                 # the box: x, y and z (the height)
  m$agent_radius <- 1.2
  n <- m$boids
  m$agents <- data.frame(x = m$random(n) * m$box_width, y = m$random(n) * m$box_height, z = m$random(n) * m$box_depth,
                         vx = m$random(n, -1, 1), vy = m$random(n, -1, 1), vz = m$random(n, -1, 1), color = NA)
  m$agents$color[1] <- "#d62728"                     # an own color (NA: agent_color)
}

m$step <- function(m) {
  a <- m$agents
  dx <- outer(a$x, a$x, function(i, j) j - i)       # the vectors to the other boids (rows: the boid)
  dy <- outer(a$y, a$y, function(i, j) j - i)
  dz <- outer(a$z, a$z, function(i, j) j - i)
  dist <- sqrt(dx^2 + dy^2 + dz^2)
  diag(dist) <- Inf
  near <- dist < m$view
  count <- pmax(rowSums(near), 1)
  has <- rowSums(near) > 0
  close <- dist < m$view / 3
  for (k in c("x", "y", "z")) {
    d <- switch(k, x = dx, y = dy, z = dz)
    v <- a[[paste0("v", k)]]
    center <- (near %*% a[[k]])[, 1] / count
    heading <- (near %*% v)[, 1] / count
    crowd <- -rowSums(close * d)
    a[[paste0("v", k)]] <- v + has * (m$cohesion * (center - a[[k]]) + m$alignment * (heading - v)) + m$separation * crowd
  }
  speed <- sqrt(a$vx^2 + a$vy^2 + a$vz^2)
  f <- ifelse(speed > m$max_speed, m$max_speed / pmax(speed, 1e-12), 1)
  a$vx <- a$vx * f; a$vy <- a$vy * f; a$vz <- a$vz * f
  a$x <- a$x + a$vx; a$y <- a$y + a$vy; a$z <- a$z + a$vz
  a$vz <- ifelse(a$z < 0 | a$z > m$box_depth, -a$vz, a$vz)    # bounce at the floor and the ceiling
  m$agents <- a
  m$wrap_in_box()                                     # x and y wrap around, z stays in the box
  m$chart("Mean speed", mean(sqrt(a$vx^2 + a$vy^2 + a$vz^2)))
}

m$action("scatter", function(m) {
  n <- nrow(m$agents)
  m$agents$vx <- m$random(n, -3, 3); m$agents$vy <- m$random(n, -3, 3); m$agents$vz <- m$random(n, -3, 3)
}, label = "Scatter the swarm")

simulation(m)
