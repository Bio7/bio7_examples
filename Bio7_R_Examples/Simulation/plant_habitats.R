# Plant species settle in the habitats which fit their Ellenberg indicator values.
#
# One step is one year, one cell 10 x 10 m. The landscape is a mosaic of six habitats with their
# site factors on the scales of Ellenberg (light L 1 - 9, moisture F 1 - 12, reaction R 1 - 9,
# nitrogen N 1 - 9; small differences within a patch): beech forest, calcareous grassland, wet
# meadow, raised bog, ruderal field margin and river bank with reed. Twelve species with their
# indicator values (NA: indifferent) and their growth: clonal species (rhizomes: nettle, reed,
# wood sorrel) grow in compact patches, species with light seeds (sundew, reed, heather) found new
# patches far away, the beech grows slowly, reproduces after 15 years and shades its neighbors
# (their light falls by 3).
#
# The suitability of a cell for a species is the product of the fits of the four factors
# (1 / (1 + (d / tolerance)^4), d the difference of the site and the indicator value). It decides
# the establishment of a seed, the survival of a plant and the displacement of a much weaker
# neighbor. Each year all seeds and shoots are collected; the first one which arrives in a cell (a
# lottery) may establish there. Without disturbance the strongest competitor of a habitat takes
# over (competitive exclusion); disturbance (gaps) keeps weaker species in the habitat. The panel
# beside the map (an ImageJ overlay: sharp at each zoom; each plant a filled oval on its habitat) summarizes each habitat: the species which fit it by their indicator values (the
# mean suitability at least 0.5: they can settle) and the species which grow there now.
#
# The indicator values after Ellenberg et al. (Zeigerwerte von Pflanzen in Mitteleuropa, Scripta
# Geobotanica 18) as commonly cited; check them against the original tables before a scientific use.

m <- grid_model("PlantHabitats", "Plant species settle in the habitats which fit their Ellenberg indicator values: a mosaic of six habitats and twelve species.")

m$param("tolerance", 1.6, min = 0.5, max = 4, step = 0.1, label = "Tolerance (Ellenberg units)", group = "Species")
m$param("seed_rain", 0.3, min = 0, max = 1, step = 0.05, label = "Seed rain from outside (per species and year)", group = "Species")
m$param("shade", TRUE, label = "Shade of the beech", group = "Species")
m$param("patches", 16, min = 6, max = 40, label = "Patches of the landscape", restart = TRUE, group = "Landscape")
m$param("disturbance", 0.05, min = 0, max = 0.1, step = 0.005, label = "Disturbance (share of the cells cleared per year)", group = "Landscape")
m$param("view", "Species", options = c("Species", "Habitats"), label = "View", group = "Landscape")

W <- 120; H <- 80; PW <- 79
# The display: each cell of the landscape is K x K cells of the image - the color of the habitat with a
# filled oval of the species (diameter 5 of 7, a border of the habitat between neighbors)
K <- 7
DOT <- outer(0:(K - 1), 0:(K - 1), function(a, b) (a - (K - 1) / 2)^2 + (b - (K - 1) / 2)^2 <= 2.5^2)
HABITATS <- c("Beech forest", "Calcareous grassland", "Wet meadow", "Raised bog", "Ruderal margin", "River bank")
SITE <- rbind(c(3, 5, 6, 5), c(8, 3, 8, 3), c(7, 8, 6, 5), c(8, 9, 1, 1), c(7, 5, 7, 8), c(7, 10, 7, 7))   # L, F, R, N
SHORT <- c("Beech", "Wood sorrel", "Woodruff", "Upright brome", "Meadow clary", "Marsh marigold", "Meadowsweet", "Sundew", "Heather",
           "Nettle", "Reed", "Oat-grass")
SPECIES <- c("Fagus sylvatica", "Oxalis acetosella", "Galium odoratum", "Bromus erectus", "Salvia pratensis", "Caltha palustris",
             "Filipendula ulmaria", "Drosera rotundifolia", "Calluna vulgaris", "Urtica dioica", "Phragmites australis", "Arrhenatherum elatius")
VALUES <- rbind(c(3, 5, NA, NA), c(1, 5, 4, 6), c(2, 5, 6, 5), c(8, 3, 8, 3), c(8, 3, 8, 4), c(7, 9, NA, 6), c(7, 8, NA, 5), c(8, 9, 1, 1),
                c(8, NA, 1, 1), c(NA, 6, 7, 8), c(7, 10, 7, 7), c(8, 5, 7, 7))   # L, F, R, N (NA: indifferent)
CLONAL <- c(0, 0.4, 0.35, 0.2, 0, 0.15, 0.3, 0, 0.1, 0.45, 0.5, 0.15)
SEEDS <- c(6, 3, 2, 10, 15, 10, 20, 30, 40, 25, 30, 25)
DISTANCE <- c(1.5, 0.8, 1.2, 2, 1, 3, 2.5, 4, 3, 2, 8, 3)
MATURITY <- c(15, 1, 1, 1, 2, 2, 2, 2, 3, 1, 2, 1)
MORTALITY <- c(0.01, 0.06, 0.06, 0.05, 0.05, 0.05, 0.05, 0.08, 0.04, 0.04, 0.03, 0.05)
HEIGHT <- c(30, 0.1, 0.2, 0.8, 0.5, 0.3, 1.5, 0.05, 0.5, 1.2, 3, 1.2)
MISFIT <- 0.25
NH <- 6; NS <- 12
HABITAT_COLORS <- c("#2e6b3a", "#d9c46a", "#7fc6a4", "#b6789a", "#c98a4b", "#5b8fc9")
SPECIES_COLORS <- c("#2f5d34", "#9fd36a", "#5fae5a", "#c9b458", "#7a5cc0", "#f2c230", "#b9a3d9", "#d1435b", "#b03a8a", "#3d6e3a", "#8a9a5b", "#cfa46b")
# the directions of the seeds (whole degrees, the same digits in Bio7 and in the page)
COS <- vapply(0:359, cos_deg, 0); SIN <- vapply(0:359, sin_deg, 0)
# the cells as vectors: index y * W + x + 1 (x, y from 0); the species 1..12, 0: empty
CX <- rep(0:(W - 1), H); CY <- rep(0:(H - 1), each = W)

# The suitability of all cells for all species (a matrix: species x cells)
fits <- function(m) {
  f <- matrix(1, NS, W * H)
  for (s in 1:NS) for (k in 1:4) {
    v <- VALUES[s, k]
    if (!is.na(v)) { d <- ((if (k == 1) m$light else m$site[k, ]) - v) / m$tolerance; f[s, ] <- f[s, ] / (1 + d^4) }
  }
  f
}

# The light: the site, minus 3 under the crowns of the adult beeches (their 8 neighbors)
shade_light <- function(m) {
  m$light <- m$site[1, ]
  if (!m$shade) return(invisible())
  adult <- which(m$species == 1 & m$age >= MATURITY[1])
  if (!length(adult)) return(invisible())
  x <- CX[adult]; y <- CY[adult]; under <- integer()
  for (dy in -1:1) for (dx in -1:1) if (dy != 0 || dx != 0) {
    ok <- x + dx >= 0 & x + dx < W & y + dy >= 0 & y + dy < H
    under <- c(under, (y[ok] + dy) * W + x[ok] + dx + 1)
  }
  under <- unique(under)
  m$light[under] <- pmax(1, m$site[1, under] - 3)
}

# The propagules s at the cells target: the first one in a cell (a lottery) establishes or displaces a weaker plant
settle <- function(m, s, target, fit) {
  if (!length(s)) return(invisible())
  o <- sample.int(length(s)); s <- s[o]; target <- target[o]
  first <- !duplicated(target); s <- s[first]; target <- target[first]
  other <- m$species[target]
  keep <- other != s; s <- s[keep]; target <- target[keep]; other <- other[keep]
  chance <- m$random(length(s))
  fit_s <- fit[cbind(s, target)]
  strength <- fit_s * HEIGHT[s] / (HEIGHT[s] + 1)
  ob <- pmax(other, 1)
  weaker <- fit[cbind(ob, target)] * HEIGHT[ob] / (HEIGHT[ob] + 1)
  new <- ifelse(other == 0, chance < fit_s, strength > 2 * weaker & chance < 0.2 * (strength - weaker) / pmax(strength, 1e-12))
  m$species[target[new]] <- s[new]
  m$age[target[new]] <- 0
}

# The species with the largest values (at least minimum, at most count): "Reed 80 %, Nettle 5 %" or "none"
names_of <- function(value, minimum, count, percent) {
  o <- order(-value); o <- o[value[o] >= minimum][seq_len(min(count, sum(value >= minimum)))]
  if (!length(o)) return("none")
  paste(paste0(SHORT[o], if (percent) paste0(" ", round(value[o]), "%") else ""), collapse = ", ")
}

show_habitats <- function(m) {
  # the map: the habitat (pale) of each cell with an oval of its species, or the habitats (view Habitats)
  block <- matrix(1L, K, K)
  habitat <- matrix(m$habitat, H, W, byrow = TRUE)
  if (m$view == "Habitats") {
    land <- kronecker(NH + NS + 1L + habitat, block)
  } else {
    land <- kronecker(habitat, block)
    species <- kronecker(matrix(m$species, H, W, byrow = TRUE), block)
    dot <- kronecker(matrix(1L, H, W), DOT * 1L) > 0 & species > 0
    land[dot] <- NH + species[dot] - 1L
  }
  grid <- matrix(NH + NS, H * K, (W + PW) * K)
  grid[, 1:(W * K)] <- land
  m$cells[] <- as.integer(grid)
  text <- function(x, y, t, color, size) m$draw_text(K * x, K * y, t, color, K * size, overlay = TRUE)
  square <- function(x, y, size, color) m$draw_rect(K * x, K * y, K * size, K * size, color, NULL, overlay = TRUE)
  # the panel as an ImageJ overlay (sharp at each zoom, Image > Overlay > Hide Overlay hides it)
  ty <- 3.6
  text(W + 2, ty, "Settled species (% of the cells)", "#111111", 3.0)
  ty <- ty + 4.6
  for (h in 1:NH) {
    inh <- m$habitat == h - 1
    share <- 100 * vapply(1:NS, function(s) sum(inh & m$species == s), 0) / max(1, sum(inh))
    m$chart_bars(paste0(HABITATS[h], " (% of the cells)"), SHORT, share)
    m$chart(HABITATS[h], sum(share >= 1), chart = "Species in the habitats")
    square(W + 2, ty - 2.2, 2.4, HABITAT_COLORS[h])
    text(W + 5.6, ty, HABITATS[h], "#111111", 2.8)
    text(W + 5.6, ty + 2.8, paste("Fits:", m$fit_names[h]), "#2e6b3a", 2.2)
    text(W + 5.6, ty + 5.4, paste("Now:", names_of(share, 1, 3, TRUE)), "#444444", 2.2)
    ty <- ty + 9.2
  }
  ty <- ty + 0.8
  text(W + 2, ty, "Species", "#111111", 3.0)
  ty <- ty + 3.6
  for (s in 1:NS) {
    lx <- W + 2 + ((s - 1) %% 3) * 25.5; ly <- ty + ((s - 1) %/% 3) * 3.2
    square(lx, ly - 2, 2.2, SPECIES_COLORS[s])
    text(lx + 3, ly, SHORT[s], "#222222", 2.5)
  }
}

m$init <- function(m) {
  m$size((W + PW) * K, H * K)
  m$wrap <- FALSE
  do.call(m$palette, as.list(c("#e3eadf", "#f3efcf", "#dceee8", "#eadfe6", "#efe5d6", "#d8e6ef", SPECIES_COLORS, "#ffffff", HABITAT_COLORS)))
  # the habitats: patches around random centers (each habitat at least twice), the borders with noise
  n <- m$patches
  m$patch_x <- m$random(n, 0, W); m$patch_y <- m$random(n, 0, H)
  m$patch_habitat <- (seq_len(n) - 1) %% NH
  ox <- m$random(1, 0, 500); oy <- m$random(1, 0, 500)
  jx <- CX + 12 * (noise(ox + CX * 0.08, oy + CY * 0.08) - 0.5)
  jy <- CY + 12 * (noise(ox + 40 + CX * 0.08, oy + CY * 0.08) - 0.5)
  best <- rep(1, W * H); bd <- rep(Inf, W * H)
  for (p in seq_len(n)) { d <- (jx - m$patch_x[p])^2 + (jy - m$patch_y[p])^2; closer <- d < bd; bd[closer] <- d[closer]; best[closer] <- p }
  m$habitat <- m$patch_habitat[best]
  m$site <- t(vapply(1:4, function(f) SITE[m$habitat + 1, f] + 1.2 * (fractal_noise(ox + 100 * (f - 1) + CX * 0.1, oy + CY * 0.1, 3) - 0.5), numeric(W * H)))
  m$species <- rep(0L, W * H); m$age <- rep(0L, W * H)
  m$light <- m$site[1, ]
  fit <- fits(m)
  # the species which can settle in each habitat (by the indicator values, without shade)
  m$fit_names <- vapply(1:NH, function(h) names_of(100 * rowMeans(fit[, m$habitat == h - 1, drop = FALSE]), 50, 4, FALSE), "")
  # the seeds of all species everywhere: they establish where the site fits (adult plants)
  s <- rep(1:NS, each = 300)
  settle(m, s, m$random_int(1, W * H, length(s)), fit)
  m$age <- ifelse(m$species > 0, MATURITY[pmax(m$species, 1)], 0L)
  shade_light(m)
  show_habitats(m)
}

m$step <- function(m) {
  fit <- fits(m)
  occ <- m$species > 0; sp <- pmax(m$species, 1)
  # mortality: the base rate and the misfit of the site
  dies <- occ & m$random(W * H) < MORTALITY[sp] + MISFIT * (1 - fit[cbind(sp, 1:(W * H))])
  m$species[dies] <- 0L
  m$age[m$species > 0] <- m$age[m$species > 0] + 1L
  shade_light(m)
  fit <- fits(m)
  sp <- pmax(m$species, 1)
  mature <- m$species > 0 & m$age >= MATURITY[sp]
  ps <- list(); pt <- list()
  # clonal growth into the four neighbors
  for (d in list(c(1, 0), c(-1, 0), c(0, 1), c(0, -1))) {
    go <- which(mature & m$random(W * H) < CLONAL[sp])
    tx <- CX[go] + d[1]; ty <- CY[go] + d[2]
    ok <- tx >= 0 & tx < W & ty >= 0 & ty < H
    ps[[length(ps) + 1]] <- sp[go][ok]; pt[[length(pt) + 1]] <- (ty * W + tx + 1)[ok]
  }
  # seeds: an exponential distance in a random direction
  src <- rep(which(mature), SEEDS[sp[mature]])
  s <- m$species[src]
  dist <- m$random_exponential(1 / DISTANCE[s], length(src))
  a <- m$random_int(0, 359, length(src)) + 1
  tx <- round(CX[src] + dist * COS[a]); ty <- round(CY[src] + dist * SIN[a])
  ok <- tx >= 0 & tx < W & ty >= 0 & ty < H
  ps[[length(ps) + 1]] <- s[ok]; pt[[length(pt) + 1]] <- (ty * W + tx + 1)[ok]
  # seeds from outside the landscape
  rain <- rep(which(m$random(NS) < m$seed_rain), each = 3)
  ps[[length(ps) + 1]] <- rain; pt[[length(pt) + 1]] <- m$random_int(1, W * H, length(rain))
  settle(m, unlist(ps), unlist(pt), fit)
  # disturbance (e.g., windthrow, game, floods): gaps for new plants
  gaps <- m$species > 0 & m$random(W * H) < m$disturbance
  m$species[gaps] <- 0L
  show_habitats(m)
}

m$action("sow_all", function(m) {
  s <- rep(1:NS, each = 40)
  settle(m, s, m$random_int(1, W * H, length(s)), fits(m))
  show_habitats(m)
}, label = "Sow all species everywhere", group = "Species")

simulation(m)
