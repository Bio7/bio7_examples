# The mathematical functions of the simulation API in R (the functions of bio7sim and base R).
#
# The grid shows a moving surface of the chosen function (m$noise, m$fractal_noise,
# m$sin_deg/m$cos_deg, m$normal_pdf, m$distance). The charts show the statistics of the grid
# (mean, sd, median, quantile), a histogram, a predator-prey system (m$rk4) and its phase plane,
# and the logistic growth (m$rk4 compared with the exact solution). m$init and the buttons print
# the other functions to the console.
#
# The functions of the model have the same names as in Java (in snake_case) and are vectorized
# (they work with vectors and matrices). For the other mathematics use base R (mean, sd, median,
# quantile, cor, solve, det, %*%, t, ...).
#
# Run the script in Bio7, start it with Play/Pause and export it with "Export Simulation as HTML..."
# (the page runs the script with webR: R in the browser).

SIZE <- 100
CAPACITY <- 100
START <- 2

m <- value_grid_model("MathFunctions", "The mathematical functions: basic functions, statistics, distributions, linear algebra, differential equations (rk4), angles and noise.")

m$param("surface", "Fractal noise", options = c("Fractal noise", "Noise", "Waves (sin_deg, cos_deg)", "Normal density", "Distance"),
        label = "Function of the grid", group = "Grid")
m$param("scale", 0.06, min = 0.02, max = 0.3, step = 0.01, label = "Scale", group = "Grid")
m$param("octaves", 4, min = 1, max = 8, label = "Octaves (fractal noise)", group = "Grid")
m$param("speed", 0.3, min = 0, max = 2, step = 0.05, label = "Speed", group = "Grid")
m$param("prey_growth", 0.6, min = 0.1, max = 1.5, step = 0.05, label = "Growth rate of the prey", group = "Differential equations")
m$param("growth_rate", 0.3, min = 0.05, max = 1, step = 0.05, label = "Rate of the logistic growth", group = "Differential equations")

# The values (vector or matrix) with 4 decimal places as text (for the console)
r4 <- function(v) {
  v <- round(v, 4) + 0
  if (is.matrix(v)) paste0("[", paste(apply(v, 1, function(row) paste0("[", paste(row, collapse = ", "), "]")), collapse = ", "), "]")
  else if (length(v) > 1) paste0("[", paste(v, collapse = ", "), "]")
  else format(v)
}

# The values of the grid: a surface which moves with the time step t (all cells at once: matrices)
fill <- function(m) {
  t <- m$t; s <- m$scale; x <- m$xs; y <- m$ys
  shift <- t * m$speed
  # the center moves on a circle (cos_deg, sin_deg: angles in degrees) and wraps around the grid (wrap_around)
  cx <- m$wrap_around(SIZE / 2 + SIZE / 4 * m$cos_deg(t * m$speed * 10) + shift, SIZE)
  cy <- SIZE / 2 + SIZE / 4 * m$sin_deg(t * m$speed * 10)
  sigma <- 1 / s
  v <- switch(m$surface,
    # fractal_noise(x, y, octaves): 0 - 1, finer details with more octaves
    "Fractal noise" = m$fractal_noise(x * s + shift * s, y * s, m$octaves),
    # noise(x, y): a smooth value noise from 0 to 1
    "Noise" = m$noise(x * s + shift * s, y * s),
    "Waves (sin_deg, cos_deg)" = 0.5 + 0.25 * m$sin_deg(x * s * 60 + t * m$speed * 20) + 0.25 * m$cos_deg(y * s * 60),
    # normal_pdf(x, mean, sd): a bell around the center (scaled to a maximum of 1)
    "Normal density" = m$normal_pdf(x, cx, sigma) * m$normal_pdf(y, cy, sigma) / m$sq(m$normal_pdf(0, 0, sigma)),
    # distance(x1, y1, x2, y2) to the center, map_range: 0 - half of the grid to 1 - 0
    m$map_range(m$distance(x, y, cx, cy), 0, SIZE / 2, 1, 0))
  # clamp(value, min, max): the values in the range of the colors ([] keeps the matrix)
  m$values[] <- m$clamp(v, 0, 1)
}

print_basics <- function(m) {
  cat("--- Basic functions ---\n")
  cat("clamp(1.5, 0, 1) =", m$clamp(1.5, 0, 1), ", lerp(0, 10, 0.25) =", m$lerp(0, 10, 0.25),
      ", map_range(5, 0, 10, 100, 200) =", m$map_range(5, 0, 10, 100, 200), "\n")
  cat("wrap_around(-1, 100) =", m$wrap_around(-1, 100), ", sq(3) =", m$sq(3), ", distance(0, 0, 3, 4) =", m$distance(0, 0, 3, 4),
      ", distance3(0, 0, 0, 1, 2, 2) =", m$distance3(0, 0, 0, 1, 2, 2), "\n")
  cat("logistic(0) =", m$logistic(0), ", logistic_growth(50, 0.1, 100) =", m$logistic_growth(50, 0.1, 100), "\n")
  cat("cos_deg(60) =", m$cos_deg(60), ", sin_deg(30) =", m$sin_deg(30), ", heading_of(1, 1) =", m$heading_of(1, 1),
      ", heading_of(-1, 0) =", m$heading_of(-1, 0), "\n")
  cat("noise(1.5, 2.5) =", r4(m$noise(1.5, 2.5)), ", fractal_noise(1.5, 2.5, 4) =", r4(m$fractal_noise(1.5, 2.5, 4)), "\n")
  cat("vectorized: sqrt(c(1, 4, 9)) =", r4(sqrt(c(1, 4, 9))), ", lerp(0, 10, c(0, 0.5, 1)) =", r4(m$lerp(0, 10, c(0, 0.5, 1))), "\n")
}

# The functions of base R (all of them are vectorized: they work with vectors and matrices)
print_r_math <- function(m) {
  cat("--- Math of R ---\n")
  cat("Constants: pi =", format(pi, digits = 16), ", exp(1) =", format(exp(1), digits = 16), ", Inf =", Inf, ", .Machine$double.eps =", .Machine$double.eps, "\n")
  cat("abs(-3.5) =", abs(-3.5), ", sign(-3.5) =", sign(-3.5), ", min(2, 7) =", min(2, 7), ", max(2, 7) =", max(2, 7),
      ", pmax(c(1, 5), c(4, 2)) =", r4(pmax(c(1, 5), c(4, 2))), "\n")
  # rounding: round rounds .5 to the even number (IEC 60559), signif: significant digits, trunc cuts off
  cat("floor(2.7) =", floor(2.7), ", ceiling(2.1) =", ceiling(2.1), ", round(2.5) =", round(2.5), ", round(3.5) =", round(3.5),
      ", round(2.567, 2) =", round(2.567, 2), ", signif(123456, 2) =", signif(123456, 2), ", trunc(-2.9) =", trunc(-2.9), "\n")
  # powers and roots
  cat("2^10 =", 2^10, ", sqrt(16) =", sqrt(16), ", 27^(1/3) =", 27^(1 / 3), "\n")
  # exponential function and logarithms (log: the natural logarithm, base: another base)
  cat("exp(1) =", exp(1), ", log(exp(1)) =", log(exp(1)), ", log10(1000) =", log10(1000), ", log2(8) =", log2(8),
      ", log(81, base = 3) =", log(81, base = 3), "\n")
  cat("expm1(1e-10) =", expm1(1e-10), ", log1p(1e-10) =", log1p(1e-10), "(exact for small x)\n")
  # trigonometry in radians; sinpi(x) = sin(pi * x) exactly
  cat("sin(pi / 6) =", format(sin(pi / 6), digits = 17), ", sinpi(1 / 6) =", sinpi(1 / 6), ", cos(60 * pi / 180) =", cos(60 * pi / 180),
      ", tan(pi / 4) =", tan(pi / 4), "\n")
  cat("asin(1) =", asin(1), ", acos(0) =", acos(0), ", atan(1) =", atan(1), ", atan2(1, -1) * 180 / pi =", atan2(1, -1) * 180 / pi, "\n")
  cat("sinh(1) =", sinh(1), ", cosh(1) =", cosh(1), ", tanh(1) =", tanh(1), "\n")
  # integer arithmetic: %/% (floor division) and %% (the sign of the divisor, as floorMod of Java)
  cat("7 %/% 2 =", 7 %/% 2, ", 7 / 2 =", 7 / 2, ", 7 %% 3 =", 7 %% 3, ", -7 %% 3 =", -7 %% 3, ", -7 %/% 2 =", -7 %/% 2, "\n")
  # combinatorics and special functions
  cat("factorial(10) =", factorial(10), ", choose(10, 3) =", choose(10, 3), ", gamma(5) =", gamma(5), ", beta(2, 3) =", beta(2, 3),
      ", lgamma(100) =", lgamma(100), "\n")
  # vectors: cumulative sums and products, differences
  cat("sum(1:4) =", sum(1:4), ", prod(1:4) =", prod(1:4), ", cumsum(1:4) =", r4(cumsum(1:4)), ", cumprod(1:4) =", r4(cumprod(1:4)),
      ", diff(c(1, 4, 9, 16)) =", r4(diff(c(1, 4, 9, 16))), ", range(c(3, 1, 2)) =", r4(range(c(3, 1, 2))), "\n")
  # special values
  cat("1 / 0 =", 1 / 0, ", sqrt(-1) =", suppressWarnings(sqrt(-1)), "(a warning), is.nan(0 / 0) =", is.nan(0 / 0), ", NA + 1 =", NA + 1,
      ", isTRUE(all.equal(0.1 + 0.2, 0.3)) =", isTRUE(all.equal(0.1 + 0.2, 0.3)), "\n")
  cat("vectorized: sin(c(0, 30, 60, 90) * pi / 180) =", r4(sin(c(0, 30, 60, 90) * pi / 180)), "\n")
}

print_statistics <- function(m) {
  cat("--- Statistics (base R) ---\n")
  sample <- c(2, 4, 4, 4, 5, 5, 7, 9)
  cat("sample =", r4(sample), "\n")
  cat("sum =", sum(sample), ", mean =", mean(sample), ", var =", r4(var(sample)), ", sd =", r4(sd(sample)),
      ", min =", min(sample), ", max =", max(sample), "\n")
  # quantile: type 7 (the default of R) interpolates linearly as quantile of Java
  cat("median =", median(sample), ", quantile(0.25) =", quantile(sample, 0.25, names = FALSE),
      ", quantile(0.75) =", quantile(sample, 0.75, names = FALSE), "\n")
  cat("normal_pdf(0, 0, 1) =", r4(m$normal_pdf(0, 0, 1)), ", normal_cdf(1.96, 0, 1) =", r4(m$normal_cdf(1.96, 0, 1)), "(dnorm, pnorm)\n")
  # linear_regression(x, y): intercept, slope and r2 of y = a + b x (also lm(y ~ x))
  x <- 1:8
  y <- c(2.1, 3.9, 6.2, 7.8, 10.1, 12.2, 13.8, 16.1)
  fit <- m$linear_regression(x, y)
  cat("linear_regression: intercept =", r4(fit[["intercept"]]), ", slope =", r4(fit[["slope"]]), ", r2 =", r4(fit[["r2"]]),
      ", cor =", r4(cor(x, y)), ", coef(lm(y ~ x)) =", r4(unname(coef(lm(y ~ x)))), "\n")
}

print_linear_algebra <- function(m) {
  # linear algebra with base R: solve, det, %*% (matrix product), t (transpose), crossprod
  cat("--- Linear algebra (base R) ---\n")
  a <- matrix(c(4, -2, 1,
                3, 6, -4,
                2, 1, 8), 3, 3, byrow = TRUE)
  b <- c(11, -21, 24)
  cat("A =", r4(a), ", b =", r4(b), "\n")
  x <- solve(a, b)
  cat("solve(A, b) =", r4(x), ", A %*% x =", r4(as.vector(a %*% x)), "\n")
  cat("det(A) =", r4(det(a)), ", t(A) =", r4(t(a)), "\n")
  cat("solve(A) =", r4(solve(a)), "\n")
  cat("A %*% solve(A) =", r4(a %*% solve(a)), "\n")
  cat("sum(b * b) =", sum(b * b), ", sqrt(sum(b^2)) =", r4(sqrt(sum(b^2))), "\n")
}

m$init <- function(m) {
  m$size(SIZE, SIZE)
  m$range(0, 1)
  m$color_map("#440154", "#3b528b", "#21908c", "#5dc963", "#fde725")   # viridis (the default)
  # the coordinates of all cells (matrices of the shape of the grid: values[y, x], from 0)
  m$xs <- matrix(rep(0:(SIZE - 1), each = SIZE), SIZE, SIZE)
  m$ys <- matrix(rep(0:(SIZE - 1), times = SIZE), SIZE, SIZE)
  # the state of the predator-prey system and of the logistic growth (rk4)
  m$predator_prey <- c(10, 5)
  m$population <- START
  fill(m)
  print_basics(m)
  print_r_math(m)
  print_statistics(m)
  print_linear_algebra(m)
}

m$step <- function(m) {
  fill(m)
  v <- m$values
  # statistics of the grid (base R)
  m$chart("Mean", mean(v))
  m$chart("Standard deviation", sd(v))
  m$chart("Median", median(v))
  q <- quantile(v, c(0.1, 0.9), names = FALSE)
  m$chart("10 % quantile", q[1])
  m$chart("90 % quantile", q[2])
  # a histogram of the grid (a bar chart, replaced in each step)
  counts <- tabulate(m$clamp(floor(v * 10), 0, 9) + 1, 10)
  m$chart_bars("Histogram of the grid", paste0("0.", 0:9), counts)
  # rk4: one Runge-Kutta step of a system of differential equations (Lotka-Volterra), 10 steps per time step
  g <- m$prey_growth
  for (k in 0:9)
    m$predator_prey <- m$rk4(function(time, y) c(g * y[1] - 0.05 * y[1] * y[2], 0.02 * y[1] * y[2] - 0.4 * y[2]),
                             m$t + k * 0.1, m$predator_prey, 0.1)
  m$chart("Prey", m$predator_prey[1], chart = "Predator-prey (rk4)")
  m$chart("Predators", m$predator_prey[2], chart = "Predator-prey (rk4)")
  # chart_xy: the phase plane (prey, predators)
  m$chart_xy("Phase plane", "Predators over prey", m$predator_prey[1], m$predator_prey[2])
  # the logistic growth: rk4 of logistic_growth(n, r, k) and the exact solution with logistic(x)
  r <- m$growth_rate
  m$population <- m$rk4(function(time, y) m$logistic_growth(y, r, CAPACITY), m$t, m$population, 1)
  exact <- CAPACITY * m$logistic(r * (m$t + 1) - log((CAPACITY - START) / START))
  m$chart("rk4", m$population, chart = "Logistic growth")
  m$chart("Exact (logistic)", exact, chart = "Logistic growth")
}

m$action("print_basics", print_basics, label = "Print the basic functions")
m$action("print_r_math", print_r_math, label = "Print the math of R")
m$action("print_statistics", print_statistics, label = "Print the statistics")
m$action("print_linear_algebra", print_linear_algebra, label = "Print the linear algebra")

simulation(m)
