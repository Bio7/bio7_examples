# A neobiota: the invasion of the Himalayan balsam (Impatiens glandulifera) along a river.
#
# An annual plant from the Himalaya which escaped from gardens and invades river banks in Central
# Europe. One step is one year, one cell 5 x 5 m. The landscape (generated from the seed): a river
# which flows from the top to the bottom, its banks, floodplain forest, meadows, a road and a
# village at the bridge; the balsam starts in a garden of the village. Each year the plants throw
# their seeds a few meters (the cell and its neighbors), seeds which fall into the river drift
# downstream (an exponential distance) onto a bank, seeds at the road jump far with vehicles and
# mowing, the seeds rest in a short-lived seed bank and the cover of a cell grows with the
# germinating seeds up to the suitability of its habitat (banks 1, floodplain forest 0.6, road
# verges 0.3, the village 0.15, meadows 0.04: the dense grass). The invasion runs fast downstream
# and slowly upstream.
#
# Control: a click (or a drag) pulls the plants and the seeds in a 5 x 5 cells square, the button
# "Control campaign" pulls half of the invaded banks. Charts: the invaded area (ha), the invaded
# share of the banks, the seed bank and the invaded cells of each habitat. All cells of a step are
# computed with matrices (rows: y from the top).

m <- grid_model("InvasivePlant", "The invasion of the Himalayan balsam along a river: seeds drift downstream, jump along the road and rest in a seed bank.")

m$param("fecundity", 400, min = 50, max = 2000, label = "Seeds per cell (full cover)", group = "Balsam", type = "double")
m$param("bank_survival", 0.3, min = 0, max = 0.9, step = 0.05, label = "Seed bank survival per year", group = "Balsam")
m$param("germination", 0.6, min = 0.1, max = 1, step = 0.05, label = "Germination", group = "Balsam")
m$param("river_drift", TRUE, label = "Drift of the seeds in the river", group = "Dispersal")
m$param("drift_distance", 30, min = 5, max = 100, label = "Mean drift distance (cells)", group = "Dispersal", type = "double")
m$param("road_jumps", 0.3, min = 0, max = 1, step = 0.05, label = "Road jumps (probability per year)", group = "Dispersal")

WATER <- 0; BANK <- 1; FOREST <- 2; MEADOW <- 3; ROAD <- 4; VILLAGE <- 5; SPARSE <- 6; DENSE <- 7
HABITATS <- c("Water", "Bank", "Floodplain forest", "Meadow", "Road verge", "Village")
SUITABILITY <- c(0, 1.0, 0.6, 0.04, 0.3, 0.15)
W <- 160; H <- 100

# The sum of the eight neighbors of a matrix (no wrap)
around <- function(a) {
  p <- matrix(0, H + 2, W + 2); p[2:(H + 1), 2:(W + 1)] <- a
  s <- matrix(0, H, W)
  for (dy in -1:1) for (dx in -1:1) if (dy != 0 || dx != 0) s <- s + p[(2:(H + 1)) + dy, (2:(W + 1)) + dx]
  s
}

show_balsam <- function(m) {
  invaded <- m$cover > 0.1
  m$cells[] <- as.integer(ifelse(m$cover > 0.5, DENSE, ifelse(invaded, SPARSE, m$habitat)))
  banks <- m$habitat == BANK
  m$chart("Invaded area (ha)", sum(invaded) * 25 / 10000)
  m$chart("Invaded banks (%)", 100 * sum(invaded & banks) / sum(banks), chart = "Banks")
  m$chart("Seeds (thousands)", sum(m$bank) / 1000, chart = "Seed bank")
  m$chart_bars("Invaded cells per habitat", HABITATS, vapply(0:5, function(k) sum(invaded & m$habitat == k), 0))
  m$record("Invaded area (ha)", sum(invaded) * 25 / 10000)
}

m$init <- function(m) {
  m$size(W, H)
  m$wrap <- FALSE
  m$palette("#4a90d9", "#a8d5a2", "#3e7b47", "#dfe9b0", "#9e9e9e", "#d2c9be", "#ee9ad4", "#a2238f")
  phase <- m$random(1, 0, 360); ox <- m$random(1, 0, 500); oy <- m$random(1, 0, 500)
  y <- 0:(H - 1); x <- 0:(W - 1)
  m$river_x <- W * 0.45 + 20 * vapply(phase + y * 3.6, sin_deg, 0) + 10 * (fractal_noise(rep(ox, H), oy + y * 0.04, 3) - 0.5)
  m$road_y <- round(H * 0.68 + 3 * vapply(x * 2.5, sin_deg, 0))
  xs <- matrix(x, H, W, byrow = TRUE); ys <- matrix(y, H, W)
  d <- abs(xs - m$river_x[ys + 1]); dy <- abs(ys - m$road_y[xs + 1])
  forest <- fractal_noise(ox + xs * 0.07, oy + ys * 0.07, 4) > 0.5
  h <- matrix(MEADOW, H, W)
  h[d < 30 & forest] <- FOREST
  h[dy < 12 & d < 28] <- VILLAGE
  h[dy == 0] <- ROAD
  h[d <= 4.5] <- BANK
  h[d <= 1.5] <- WATER
  m$habitat <- h
  m$near_water <- around(h == WATER) > 0 | h == WATER
  m$cover <- matrix(0, H, W); m$bank <- matrix(0, H, W)
  # the garden: in the village, near the river, upstream of the bridge (rows and columns from 1)
  gy <- m$road_y[floor(m$river_x[1]) + 1] - 9
  gx <- round(m$river_x[gy + 1] + 9)
  m$cover[(gy + 1) + (-1:1), (gx + 1) + (-1:1)] <- 1
  show_balsam(m)
}

m$step <- function(m) {
  n <- m$cover * m$fecundity
  seeds <- matrix(0, H, W)
  add <- function(rows, cols, v) {
    ok <- rows >= 1 & rows <= H & cols >= 1 & cols <= W
    if (any(ok)) {
      s <- tapply(rep_len(v, length(ok))[ok], (cols[ok] - 1) * H + rows[ok], sum)
      seeds[as.integer(names(s))] <<- seeds[as.integer(names(s))] + s
    }
  }
  # seeds which fall into the river drift downstream (three packets of each cell)
  if (m$river_drift) {
    idx <- which(n > 0 & m$near_water, arr.ind = TRUE)
    if (nrow(idx)) {
      drift <- n[idx] * 0.3
      n[idx] <- n[idx] - drift
      k <- nrow(idx)
      y2 <- rep(idx[, 1] - 1, each = 3) + round(m$random_exponential(1 / m$drift_distance, 3 * k))
      side <- ifelse(m$random(3 * k) < 0.5, 1, -1)
      v <- rep(drift / 3, each = 3)
      ok <- y2 < H
      y2 <- y2[ok]
      x2 <- round(m$river_x[y2 + 1] + side[ok] * (2 + m$random_int(0, 2, length(y2))))
      add(y2 + 1, x2 + 1, v[ok])
    }
  }
  # rare jumps along the road
  idx <- which(n > 0 & m$habitat == ROAD, arr.ind = TRUE)
  if (nrow(idx)) {
    jump <- m$random(nrow(idx)) < m$road_jumps
    if (any(jump)) {
      x2 <- idx[jump, 2] - 1 + m$random_int(-60, 60, sum(jump))
      x2 <- x2[x2 >= 0 & x2 < W]
      if (length(x2)) add(m$road_y[x2 + 1] + 1, x2 + 1, 20)
    }
  }
  # the explosive capsules: the cell and its eight neighbors
  seeds <- seeds + 0.4 * n + 0.6 / 8 * around(n)
  seeds[m$habitat == WATER] <- 0
  # the seed bank, the germination in spring and the cover of the year
  m$bank <- m$bank * m$bank_survival + seeds
  germinating <- m$bank * m$germination
  m$bank <- m$bank - germinating
  m$cover[] <- SUITABILITY[m$habitat + 1] * germinating / (germinating + 50)
  show_balsam(m)
}

# Pulls the plants and the seeds in a 5 x 5 cells square (positions from 0)
pull <- function(m, x, y) {
  r <- max(1, floor(y) - 1):min(H, floor(y) + 3); c <- max(1, floor(x) - 1):min(W, floor(x) + 3)
  m$cover[r, c] <- 0; m$bank[r, c] <- 0
}
m$click <- function(m, x, y) { pull(m, x, y); show_balsam(m) }
m$drag <- function(m, x, y) { pull(m, x, y); show_balsam(m) }

m$action("campaign", function(m) {
  hit <- m$habitat == BANK & m$cover > 0.1 & matrix(m$random(H * W), H, W) < 0.5
  m$cover[hit] <- 0
  m$bank[hit] <- m$bank[hit] * 0.3
  show_balsam(m)
}, label = "Control campaign (half of the invaded banks)", group = "Control")

simulation(m)
