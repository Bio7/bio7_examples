# Hot air balloons over Cappadocia (Turkey): 3D shapes on the WorldWind globe of Bio7.
#
# The balloons climb to their cruising altitude and drift with the wind, which turns and becomes
# stronger with the altitude. In Bio7 they are OpenGL spheres in their altitude (no ImageJ), in the
# exported page points on an online map. The balloons are the rows of the data frame m$agents.

START_LON <- 34.829; START_LAT <- 38.643
COLORS <- c("#e63946", "#f4a261", "#e9c46a", "#2a9d8f", "#457b9d", "#9b5de5", "#f15bb5", "#00bbf9")

m <- geo_agent_model("HotAirBalloons", "Hot air balloons over Cappadocia: they climb to their cruising altitude and drift with the wind, which turns and becomes stronger with the altitude.")

m$param("balloons", 60, min = 5, max = 150, label = "Number of balloons", restart = TRUE, group = "Balloons")
m$param("max_altitude", 1200, min = 200, max = 2000, step = 50, label = "Maximum cruising altitude (m)", restart = TRUE, group = "Balloons", type = "double")
m$param("flight_time", 60, min = 20, max = 120, label = "Flight time (minutes)", group = "Balloons")
m$param("wind_from", 40, min = 0, max = 360, step = 5, label = "Wind from at the ground (degrees, 0 north)", group = "Wind", type = "double")
m$param("wind_speed", 2, min = 0, max = 8, step = 0.1, label = "Wind speed at the ground (m/s)", group = "Wind")
m$param("veering", 60, min = -120, max = 120, step = 5, label = "Turning of the wind (degrees per 1000 m)", group = "Wind", type = "double")

# n new balloons (a data frame: the columns of the agents and own columns)
new_balloons <- function(m, n, colors, minutes = 0) {
  data.frame(lon = START_LON + m$random(n, -0.03, 0.03), lat = START_LAT + m$random(n, -0.02, 0.02), altitude = 0,
             cruise = m$random(n, 150, m$max_altitude), minutes = minutes, landed = FALSE, color = colors)
}

m$init <- function(m) {
  m$area(38.50, 34.65, 38.80, 35.05)                 # south, west, north, east
  m$agent_shape <- "sphere"                          # OpenGL 3D shapes on the globe (point, sphere, cone, box, cylinder)
  m$agent_size_meters <- 120
  m$view_pitch <- 65                                 # an oblique view
  m$agent_size <- 5                                  # the size of the points on the map (pixels)
  n <- m$balloons
  # the start is spread over 15 minutes
  m$agents <- new_balloons(m, n, rep_len(COLORS, n), -(m$random_int(0, 14, n)))
}

m$step <- function(m) {
  a <- m$agents
  a$minutes <- a$minutes + 1
  active <- !a$landed & a$minutes >= 0
  # climb to the cruising altitude (2 m/s), descend at the end of the flight
  down <- active & a$minutes > m$flight_time
  up <- active & !down & a$altitude < a$cruise
  a$altitude[down] <- pmax(0, a$altitude[down] - 90)
  a$altitude[up] <- pmin(a$cruise[up], a$altitude[up] + 120)
  landing <- down & a$altitude == 0
  a$landed[landing] <- TRUE
  a$color[landing] <- "#8d8d8d"
  fly <- which(active & !landing)
  # the wind in the altitude of the balloons: it turns and becomes stronger
  from <- m$wind_from + m$veering * a$altitude[fly] / 1000
  speed <- m$wind_speed * (1 + a$altitude[fly] / 800) + m$random_gaussian(length(fly), 0, 0.3)
  km <- speed * 60 / 1000
  m$agents <- a
  # the wind blows from "from": the movement is in the opposite direction
  if (length(fly)) m$move(fly, -m$sin_deg(from) * km, -m$cos_deg(from) * km)
  m$chart("Balloons in the air", length(fly))
  m$chart("Mean altitude (m)", if (length(fly)) mean(a$altitude[fly]) else 0)
}

m$action("new_group", function(m) {
  m$agents <- rbind(m$agents, new_balloons(m, 10, COLORS[m$random_int(1, length(COLORS), 10)]))
}, label = "Start a new group of balloons")

simulation(m)
