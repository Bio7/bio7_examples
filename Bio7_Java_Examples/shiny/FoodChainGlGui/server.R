# The server of the food chain app (WindowBuilder Shiny with 3D; the design:
# src/gui/windowbuilder/FoodChainGlGui.java, ui.R is created from it). Bio7 writes this file only
# once: this code stays when ui.R is created again.
#
# The canvas glAttractor of the design (its name starts with "gl") is a 3D view of
# the R package rgl: rgl::rglwidgetOutput in ui.R, rgl::renderRglwidget here
# (install.packages("rgl")). Drag the view with the mouse to rotate it.
#
# The food chain of Hastings and Powell (1991): prey x, predator y, top predator z.
options(rgl.useNULL = TRUE)
library(shiny)

# The model with Runge-Kutta 4 (a step of 0.1); without the transient (a part of the time)
simulate <- function(b1, time, transient) {
  a1 <- 5; a2 <- 0.1; b2 <- 2; d1 <- 0.4; d2 <- 0.01; dt <- 0.1
  f <- function(s) {
    f1 <- a1 * s[1] / (1 + b1 * s[1])
    f2 <- a2 * s[2] / (1 + b2 * s[2])
    c(s[1] * (1 - s[1]) - f1 * s[2], f1 * s[2] - f2 * s[3] - d1 * s[2], f2 * s[3] - d2 * s[3])
  }
  n <- round(time / dt)
  out <- matrix(NA_real_, n + 1, 3)
  s <- c(0.8, 0.2, 8)
  out[1, ] <- s
  for (i in seq_len(n)) {
    k1 <- f(s); k2 <- f(s + dt / 2 * k1); k3 <- f(s + dt / 2 * k2); k4 <- f(s + dt * k3)
    s <- s + dt / 6 * (k1 + 2 * k2 + 2 * k3 + k4)
    out[i + 1, ] <- s
  }
  keep <- seq(floor(nrow(out) * transient) + 1, nrow(out))
  data.frame(time = (keep - 1) * dt, x = out[keep, 1], y = out[keep, 2], z = out[keep, 3])
}

# The local maxima of a series (a cycle has few different maxima, chaos many)
maxima <- function(z) z[which(diff(sign(diff(z))) == -2) + 1]

function(input, output, session) {

  data <- reactive({
    input$btnRun
    simulate(input$scaleB1 / 10, input$spinnerTime, input$scaleTransient / 100)
  })

  # The colors of the curve: by the time or by the top predator
  colors <- reactive({
    d <- data()
    v <- if (identical(input$comboColor, "Top predator")) d$z else d$time
    v <- (v - min(v)) / max(diff(range(v)), 1e-9)
    hcl.colors(100, "Spectral", rev = TRUE)[1 + floor(v * 99)]
  })

  # The 3D view (rgl, WebGL in the browser)
  output$glAttractor <- rgl::renderRglwidget({
    d <- data()
    rgl::open3d(useNULL = TRUE)
    rgl::lines3d(d$x, d$y, d$z, col = colors())
    rgl::axes3d()
    rgl::title3d(xlab = "Prey", ylab = "Predator", zlab = "Top predator")
    widget <- rgl::rglwidget()
    rgl::close3d()
    widget
  })

  # The normal canvas: a plot
  output$canvasSeries <- renderPlot({
    d <- data()
    plot(d$time, d$z, type = "l", col = "#C44E52", xlab = "Time", ylab = "Top predator", main = "The top predator over time")
  })

  output$outSummary <- renderText({
    d <- data()
    m <- maxima(d$z)
    k <- length(unique(round(m, 2)))
    kind <- if (length(m) < 3) "a stable state"
            else if (k == 1) "a cycle (one maximum of the top predator)"
            else if (k <= 4) sprintf("a cycle (%d different maxima of the top predator)", k)
            else sprintf("chaos (%d different maxima of the top predator)", k)
    sprintf("b1 = %.1f: %s; the top predator varies between %.2f and %.2f.", input$scaleB1 / 10, kind, min(d$z), max(d$z))
  })
}
