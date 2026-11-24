# An epidemic on a grid (SIR with immunity): all kinds of settings (sliders, a number field,
# a checkbox, a choice, a text field), groups and buttons.
#
# Run the script in Bio7, start it with Play/Pause and export it with "Export Simulation as HTML...".

SUSCEPTIBLE <- 0L; INFECTED <- 1L; RECOVERED <- 2L; VACCINATED <- 3L

m <- grid_model("Epidemic", "An epidemic on a grid: infected cells infect their susceptible neighbors, recover after some days and lose their immunity again.")

# World: a slider, a checkbox, a choice
m$param("grid_size", 120, min = 40, max = 250, label = "Grid size (cells)", restart = TRUE, group = "World")
m$param("torus", TRUE, label = "Torus world (wrap at the borders)", group = "World")
m$param("neighborhood", "Moore (8 cells)", options = c("Moore (8 cells)", "von Neumann (4 cells)"), label = "Neighborhood", group = "World")
# Disease: a slider and number fields (no min and max)
m$param("infection", 0.25, min = 0, max = 1, step = 0.01, label = "Infection probability (per infected neighbor)", group = "Disease")
m$param("recovery_days", 7L, label = "Days until the recovery", group = "Disease")
m$param("immunity_days", 60L, label = "Days of immunity (0: for ever)", group = "Disease")
# Start: a text field and a choice
m$param("initial_infected", "60,60; 20,100", label = "Infected cells at the start (x,y; x,y)", restart = TRUE, group = "Start")
m$param("colors", "Classic", options = c("Classic", "Pastel"), label = "Colors", group = "Start")
# Interventions: a slider and the buttons below
m$param("vaccine_share", 0.1, min = 0, max = 0.5, step = 0.01, label = "Share of a vaccination", group = "Interventions")

apply_colors <- function(m) {
  if (m$colors == "Pastel") m$palette("#e8f1fb", "#f28b82", "#a8dab5", "#c5b3e6")
  else m$palette("#f2f2f2", "#d62728", "#2ca02c", "#1f77b4")
}

m$init <- function(m) {
  m$size(m$grid_size, m$grid_size)
  m$days <- matrix(0L, m$height, m$width)            # the days in the state
  apply_colors(m)
  for (part in strsplit(m$initial_infected, ";")[[1]]) {
    xy <- as.integer(strsplit(part, ",")[[1]])
    if (length(xy) == 2 && xy[1] >= 1 && xy[2] >= 1 && xy[1] <= m$width && xy[2] <= m$height) m$set(xy[1], xy[2], INFECTED)
  }
}

m$step <- function(m) {
  m$wrap <- m$torus
  apply_colors(m)
  c <- m$cells
  k <- if (startsWith(m$neighborhood, "Moore")) m$count_neighbors(INFECTED) else m$count4(INFECTED)
  # the probability of an infection by k infected neighbors: 1 - (1 - p)^k
  infected <- c == SUSCEPTIBLE & m$random(length(c)) < 1 - (1 - m$infection)^k
  days <- m$days + 1L
  recovered <- c == INFECTED & days >= m$recovery_days
  susceptible <- c == RECOVERED & m$immunity_days > 0 & days >= m$immunity_days
  new <- c
  new[infected] <- INFECTED; new[recovered] <- RECOVERED; new[susceptible] <- SUSCEPTIBLE
  m$days <- ifelse(new != c, 0L, days)
  m$cells <- new
  m$chart("Susceptible", m$count(SUSCEPTIBLE))
  m$chart("Infected", m$count(INFECTED))
  m$chart("Recovered", m$count(RECOVERED))
}

m$action("infect_random", function(m) m$set(m$random_int(1, m$width), m$random_int(1, m$height), INFECTED),
         label = "Infect a random cell", group = "Interventions")
m$action("vaccinate", function(m) {
  chosen <- m$cells == SUSCEPTIBLE & m$random(length(m$cells)) < m$vaccine_share
  m$cells[chosen] <- VACCINATED
}, label = "Vaccinate the susceptible cells (share)", group = "Interventions")

simulation(m)
