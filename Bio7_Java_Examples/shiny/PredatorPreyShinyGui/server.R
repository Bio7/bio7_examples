# The server of the predator-prey app (WindowBuilder Shiny example; the design:
# src/gui/shiny/PredatorPreyShinyGui.java, ui.R is created from it). Bio7 writes this
# file only once: this code stays when ui.R is created again.
#
#   dN/dt = r N (1 - N / K) - a N P        (the prey, logistic growth)
#   dP/dt = b a N P - m P                   (the predators)
library(shiny)

function(input, output, session) {

  # The parameters of the sliders (the rates in 1/100, the attack rate in 1/1000)
  params <- reactive(c(r = input$scaleR / 100, K = input$scaleK, a = input$scaleA / 1000,
                       b = input$scaleB / 100, m = input$scaleM / 100))

  # The model solved with Runge-Kutta 4 (a step of 0.05 years)
  solution <- reactive({
    p <- params()
    dt <- 0.05
    n <- round(input$spinnerYears / dt)
    f <- function(s) c(p[["r"]] * s[1] * (1 - s[1] / p[["K"]]) - p[["a"]] * s[1] * s[2],
                       p[["b"]] * p[["a"]] * s[1] * s[2] - p[["m"]] * s[2])
    s <- matrix(NA_real_, n + 1, 2)
    s[1, ] <- c(input$spinnerN0, input$spinnerP0)
    for (i in seq_len(n)) {
      y <- s[i, ]
      k1 <- f(y); k2 <- f(y + dt / 2 * k1); k3 <- f(y + dt / 2 * k2); k4 <- f(y + dt * k3)
      s[i + 1, ] <- pmax(0, y + dt / 6 * (k1 + 2 * k2 + 2 * k3 + k4))
    }
    data.frame(time = seq(0, by = dt, length.out = n + 1), prey = s[, 1], predators = s[, 2])
  })

  # The equilibrium with predators: N* = m / (b a), P* = r / a (1 - N* / K)
  equilibrium <- reactive({
    p <- params()
    n <- p[["m"]] / (p[["b"]] * p[["a"]])
    c(N = n, P = p[["r"]] / p[["a"]] * (1 - n / p[["K"]]))
  })

  colors <- c(prey = "#4C72B0", predators = "#C44E52")

  output$canvasSeries <- renderPlot({
    s <- solution()
    matplot(s$time, s[, c("prey", "predators")], type = "l", lty = 1, lwd = 2, col = colors,
            xlab = "Years", ylab = "Population", main = "Prey and predators")
    legend("topright", c("Prey", "Predators"), col = colors, lwd = 2, bty = "n")
    e <- equilibrium()
    if (isTRUE(input$chkEquilibrium) && e[["P"]] > 0) abline(h = e, col = colors, lty = 2)
  })

  output$canvasPhase <- renderPlot({
    s <- solution()
    p <- params()
    e <- equilibrium()
    plot(s$prey, s$predators, type = "l", lwd = 2, col = "#55A868", xlab = "Prey", ylab = "Predators",
         main = "Phase plane", xlim = c(0, max(s$prey, p[["K"]])), ylim = c(0, max(s$predators, e[["P"]], 1)))
    points(s$prey[1], s$predators[1], pch = 19)
    points(tail(s$prey, 1), tail(s$predators, 1), pch = 17, col = colors[["predators"]])
    if (isTRUE(input$chkEquilibrium)) {
      # The nullclines: the prey don't change on P = r / a (1 - N / K), the predators on N = N*
      curve(p[["r"]] / p[["a"]] * (1 - x / p[["K"]]), from = 0, to = p[["K"]], add = TRUE, lty = 2, col = colors[["prey"]])
      abline(v = e[["N"]], lty = 2, col = colors[["predators"]])
      if (e[["P"]] > 0) points(e[["N"]], e[["P"]], pch = 4, cex = 2, lwd = 2)
    }
  })

  output$outSummary <- renderText({
    s <- solution()
    e <- equilibrium()
    if (e[["P"]] <= 0) {
      sprintf("The predators can't persist: their equilibrium needs %.1f prey, more than the carrying capacity.", e[["N"]])
    } else {
      sprintf("Equilibrium: %.1f prey and %.1f predators; after %d years: %.1f prey and %.1f predators.",
              e[["N"]], e[["P"]], input$spinnerYears, tail(s$prey, 1), tail(s$predators, 1))
    }
  })

  output$tableEquilibrium <- renderTable({
    s <- solution()
    e <- equilibrium()
    data.frame(Quantity = c("Prey at the equilibrium", "Predators at the equilibrium", "Prey (minimum)", "Prey (maximum)",
                            "Predators (minimum)", "Predators (maximum)"),
               Value = c(e[["N"]], max(0, e[["P"]]), min(s$prey), max(s$prey), min(s$predators), max(s$predators)))
  }, digits = 1)
}
