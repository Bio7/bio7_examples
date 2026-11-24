# An epidemic (SIR) on a network of contacts.
#
# The people are the rows of m$agents; an infected person (red) infects each susceptible
# contact (blue) with a probability per step and recovers (grey) with another probability.
# The contacts are the links of a network (drawn as lines; a link which passed on the
# infection turns red):
# - "Moving contacts": the people walk; in each step all pairs closer than the contact distance
#   are linked (m$neighbor_pairs: a spatial index, fast for thousands of people),
# - "Small world", "Scale-free" and "Random": a fixed network (m$link_small_world,
#   m$link_scale_free, m$link_random), arranged with a spring layout (m$layout_spring).
#
# Charts: the susceptible, infected and recovered people and the number of contacts. The
# simulation stops when no one is infected (m$stop()). The model records the share of the infected people (m$record) for experiments: the button
# "Experiment..." of the settings (e.g. the infection 0.02:0.1:0.02 with 5 repetitions) or
# network_epidemic_experiment.R.
#
# A click on the floor of the 3D view (also in the page) infects the nearest person (m$click).

m <- agent_model("NetworkEpidemic", "An epidemic (SIR) on a network of contacts: moving people (a spatial index) or a small-world, scale-free or random network.")

m$param("people", 400, min = 50, max = 3000, label = "People", restart = TRUE)
m$param("network", "Moving contacts", options = c("Moving contacts", "Small world", "Scale-free", "Random"), label = "Network", restart = TRUE)
m$param("contacts", 6, min = 2, max = 20, label = "Contacts of a person (fixed networks)", restart = TRUE)
m$param("distance", 4, min = 1, max = 15, step = 0.5, label = "Contact distance (moving)", group = "Moving", type = "double")
m$param("speed", 0.8, min = 0, max = 3, step = 0.1, label = "Speed of the people", group = "Moving", type = "double")
m$param("infection", 0.05, min = 0, max = 0.5, step = 0.005, label = "Infection per contact and step", group = "Disease", type = "double")
m$param("recovery", 0.04, min = 0, max = 0.3, step = 0.005, label = "Recovery per step", group = "Disease", type = "double")
m$param("initial", 3, min = 1, max = 30, label = "Infected at the start", group = "Disease", restart = TRUE)

COLORS <- c(S = "#2f6fed", I = "#e53935", R = "#9aa0a6")

show_people <- function(m) {
  st <- m$agents$state
  m$agents$color <- unname(COLORS[st])
  n <- length(st); infected <- sum(st == "I")
  m$peak <- max(m$peak, infected)
  m$chart("Susceptible", sum(st == "S"))
  m$chart("Infected", infected)
  m$chart("Recovered", sum(st == "R"))
  m$chart("Contacts", nrow(m$links()), chart = "Network")
  # for experiments: the share of the people who were infected and the peak
  m$record("Infected share", sum(st != "S") / max(1, n))
  m$record("Peak share", m$peak / max(1, n))
}

m$init <- function(m) {
  m$box(100, 100, 4)
  m$floor <- "grid"
  m$agent_radius <- 0.9
  n <- m$people
  m$agents <- data.frame(x = m$random(n, 0, 100), y = m$random(n, 0, 100), z = rep(2, n),
                         state = rep("S", n), angle = m$random(n, 0, 360), stringsAsFactors = FALSE)
  if (m$network == "Small world") m$link_small_world(m$contacts, 0.1)
  else if (m$network == "Scale-free") m$link_scale_free(max(1, m$contacts %/% 2))
  else if (m$network == "Random") m$link_random(m$contacts / max(1, n - 1))
  if (m$network != "Moving contacts") {
    m$layout_circle()
    m$layout_spring(80)
  }
  for (k in seq_len(min(n, m$initial))) m$agents$state[floor(m$random(1) * n) + 1] <- "I"
  m$peak <- 0
  show_people(m)
}

m$step <- function(m) {
  a <- m$agents
  if (m$network == "Moving contacts") {
    # the people walk (a torus), the contacts of this step: all pairs within the distance
    a$angle <- a$angle + m$random(nrow(a), -30, 30)
    a$x <- (a$x + m$speed * cos_deg(a$angle)) %% m$box_width
    a$y <- (a$y + m$speed * sin_deg(a$angle)) %% m$box_height
    m$agents <- a
    m$clear_links()
    pr <- m$neighbor_pairs(m$distance)
    m$link(pr[, 1], pr[, 2])
  }
  # the infections along the links (all at once): a link between an infected and a susceptible person
  l <- m$links()
  st <- a$state
  hot <- (st[l[, 1]] == "I" & st[l[, 2]] == "S") | (st[l[, 1]] == "S" & st[l[, 2]] == "I")
  pass <- hot & m$random(nrow(l)) < m$infection
  if (any(pass)) {
    m$set_link_color(l[pass, 1], l[pass, 2], "#e53935")
    target <- ifelse(st[l[pass, 1]] == "S", l[pass, 1], l[pass, 2])
    new <- unique(target)
  } else new <- integer()
  recovered <- which(st == "I" & m$random(length(st)) < m$recovery)
  st[new] <- "I"
  st[recovered] <- "R"
  m$agents$state <- st
  show_people(m)
  # the epidemic is over: the simulation stops (an experiment ends the run)
  if (!any(st == "I")) m$stop()
}

m$action("infect", function(m) {
  i <- floor(m$random(5) * nrow(m$agents)) + 1
  m$agents$state[i] <- "I"
  show_people(m)
}, label = "Infect 5 people")

# A click on the floor of the view: the nearest person (within 8) is infected
m$click <- function(m, x, y) {
  d <- (m$agents$x - x)^2 + (m$agents$y - y)^2
  i <- which.min(d)
  if (length(i) && d[i] < 64) {
    m$agents$state[i] <- "I"
    show_people(m)
  }
}

simulation(m)
