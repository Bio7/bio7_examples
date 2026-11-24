# The zone of influence model of plant competition (after Schwinning and Weiner 1998 and Weiner et al. 2001).
#
# Each plant takes up resources in a circle around it, its zone of influence, which grows with its
# mass m (the area A = c * m^(2/3)). Where zones overlap, the plants share the resource of a cell:
# complete symmetry (equal shares), size-symmetric (shares in proportion to the mass),
# size-asymmetric (in proportion to m^4: larger plants get much more, e.g., light) or complete
# asymmetry (the largest plant gets all). The growth per step: dm = r * (A_eff - c * m^2 / M^(4/3));
# A_eff is the uptake area after the sharing, M the size of a plant without neighbors. A plant which
# gets less than 5 % of the resources of its zone five steps in a row starves and dies (or when its
# mass falls below 0.5): self-thinning. Asymmetric competition makes few large and many small plants
# (a high Gini coefficient of the sizes), symmetric competition plants of similar sizes.
#
# The grid shows the crowding (the number of zones over a cell), the circles the zones. A click plants
# a seedling (positions from 0). Charts: the plants alive, the mean mass, the Gini coefficient and the
# size classes. The plants are the rows of the data frame m$stand.

m <- value_grid_model("ZoneOfInfluence", "The zone of influence model of plant competition: overlapping zones share the resources, symmetric or asymmetric.")

m$param("plants", 250, min = 20, max = 800, label = "Plants at the start", restart = TRUE, group = "Plants")
m$param("competition", "Size-asymmetric", options = c("Complete symmetry", "Size-symmetric", "Size-asymmetric", "Complete asymmetry"),
        label = "Competition", group = "Plants")
m$param("rate", 0.6, min = 0.1, max = 2, step = 0.05, label = "Growth rate r", group = "Plants")
m$param("max_mass", 1000, min = 100, max = 3000, step = 50, label = "Size without neighbors M", group = "Plants", type = "double")
m$param("max_radius", 8, min = 3, max = 15, step = 0.5, label = "Radius of the zone at M (cells)", restart = TRUE, group = "Plants")

W <- 100; H <- 100

# The cube root with the same digits in Bio7 and in the page (Newton's method; vectorized)
cbrt <- function(v) {
  r <- ifelse(v < 1, 1, v / 3 + 1)
  for (k in 1:60) r <- (2 * r + v / (r * r)) / 3
  ifelse(v <= 0, 0, r)
}
area <- function(m, mass) { cm <- cbrt(mass); m$c * cm * cm }

# The cells of the zone of a plant: row and column indices (from 1) of the cells whose center is within r, and the cell of the plant
zone <- function(x, y, r) {
  cols <- max(0, floor(x - r)):min(W - 1, floor(x + r)); rows <- max(0, floor(y - r)):min(H - 1, floor(y + r))
  g <- expand.grid(col = cols, row = rows)
  inside <- (g$col + 0.5 - x)^2 + (g$row + 0.5 - y)^2 <= r * r | (g$col == floor(x) & g$row == floor(y))
  cbind(g$row[inside] + 1, g$col[inside] + 1)
}

show_zones <- function(m) {
  p <- m$stand[m$stand$alive, ]
  r <- sqrt(area(m, p$mass) / pi)
  big <- p$mass / m$max_mass > 0.5
  for (k in seq_len(nrow(p))) m$draw_circle(p$x[k], p$y[k], r[k], if (big[k]) "#2e7d3240" else "#8bc34a33", if (big[k]) "#1b5e20" else "#689f38")
  n <- nrow(p); mean <- if (n) mean(p$mass) else 0
  gini <- if (n > 1 && mean > 0) sum(abs(outer(p$mass, p$mass, "-"))) / (2 * n * n * mean) else 0
  classes <- tabulate(pmin(9, floor(p$mass / m$max_mass * 10)) + 1, 10)
  m$chart("Plants alive", n)
  m$chart("Mean mass", mean, chart = "Size")
  m$chart("Gini coefficient", gini, chart = "Inequality")
  m$chart_bars("Size classes (mass)", paste("<", round((1:10) * m$max_mass / 10)), classes)
  m$record("Gini coefficient", gini)
  m$record("Plants alive", n)
}

m$init <- function(m) {
  m$size(W, H)
  m$wrap <- FALSE
  m$range(0, 6)
  m$color_map("#f7f4ea", "#e6dfb8", "#c9b97a", "#a3884a", "#6e5a2c")
  cm <- cbrt(m$max_mass)
  # c: the area of the zone of a plant of the size M is pi * radius^2
  m$c <- pi * m$max_radius * m$max_radius / (cm * cm)
  n <- m$plants
  m$stand <- data.frame(x = m$random(n, 0, W), y = m$random(n, 0, H), mass = m$random(n, 0.8, 1.2), alive = TRUE, starving = 0)
  show_zones(m)
}

m$step <- function(m) {
  p <- m$stand
  pw <- switch(m$competition, "Complete symmetry" = 0, "Size-symmetric" = 1, 4)
  complete <- m$competition == "Complete asymmetry"
  # the weights as products (the same digits in Bio7 and in the page)
  w <- if (pw == 0) rep(1, nrow(p)) else if (pw == 1) p$mass else p$mass * p$mass * p$mass * p$mass
  r <- sqrt(area(m, p$mass) / pi)
  crowd <- matrix(0, H, W); weights <- matrix(0, H, W); largest <- matrix(0, H, W); owner <- matrix(0L, H, W)
  live <- which(p$alive)
  zones <- vector("list", nrow(p))
  # the zones over the cells: the crowding, the sum of the weights, the largest plant
  for (i in live) {
    z <- zone(p$x[i], p$y[i], r[i]); zones[[i]] <- z
    crowd[z] <- crowd[z] + 1
    weights[z] <- weights[z] + w[i]
    bigger <- p$mass[i] > largest[z]
    largest[z[bigger, , drop = FALSE]] <- p$mass[i]
    owner[z[bigger, , drop = FALSE]] <- i
  }
  # the uptake area after the sharing and the growth
  a <- area(m, p$mass)
  cm <- cbrt(m$max_mass); m43 <- cm * cm * cm * cm
  effective <- rep(0, nrow(p))
  for (i in live) {
    z <- zones[[i]]
    share <- if (complete) sum(owner[z] == i) else sum(w[i] / weights[z])
    effective[i] <- a[i] * share / nrow(z)
  }
  growth <- m$rate * (effective - m$c * p$mass * p$mass / m43)
  p$mass <- ifelse(p$alive, p$mass + growth, p$mass)
  p$starving <- ifelse(p$alive & effective < 0.05 * a, p$starving + 1, 0)
  p$alive <- p$alive & !(p$starving >= 5 | p$mass < 0.5)
  m$stand <- p
  m$values[] <- crowd
  show_zones(m)
}

# A click: a seedling at the position (positions from 0)
m$click <- function(m, x, y) {
  m$stand <- rbind(m$stand, data.frame(x = x, y = y, mass = 1, alive = TRUE, starving = 0))
  show_zones(m)
}

m$action("sow", function(m) {
  m$stand <- rbind(m$stand, data.frame(x = m$random(50, 0, W), y = m$random(50, 0, H), mass = 1, alive = TRUE, starving = 0))
  show_zones(m)
}, label = "Sow 50 seedlings", group = "Plants")

simulation(m)
