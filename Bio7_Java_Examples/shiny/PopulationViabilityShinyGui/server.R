# The server of the population viability app (WindowBuilder Shiny example; the design:
# src/shinygui/PopulationViabilityShinyGui.java, ui.R is created from it). Bio7 writes
# this file only once: this code stays when ui.R is created again.
#
# Each year every individual gets Poisson offspring (the birth rate falls with the
# density: b (1 - N / K)) and dies with the probability d; a catastrophe kills each
# individual with the probability 0.5. Many runs give the risk of extinction.
library(shiny)

function(input, output, session) {

  # The runs: a matrix (runs x years); new runs with the button or when an input changes
  runs <- reactive({
    input$btnRun
    b <- input$scaleBirth / 100
    d <- input$scaleDeath / 100
    catastrophe <- input$scaleCatastrophe / 100
    K <- input$spinnerK
    years <- input$spinnerYears
    nRuns <- input$spinnerRuns
    N <- matrix(0L, nRuns, years + 1)
    N[, 1] <- input$spinnerN0
    for (t in seq_len(years)) {
      n <- N[, t]
      births <- rpois(nRuns, n * b * pmax(0, 1 - n / K))
      deaths <- rbinom(nRuns, n, d)
      n <- pmax(0L, n + births - deaths)
      hit <- runif(nRuns) < catastrophe
      n[hit] <- rbinom(sum(hit), n[hit], 0.5)
      N[, t + 1] <- n
    }
    N
  })

  output$canvasRuns <- renderPlot({
    N <- runs()
    logarithmic <- identical(input$grpScale, "Logarithmic")
    years <- 0:(ncol(N) - 1)
    shown <- N[seq_len(min(100, nrow(N))), , drop = FALSE]
    y <- if (logarithmic) shown + 1 else shown
    matplot(years, t(y), type = "l", lty = 1, col = adjustcolor("#4C72B0", 0.25), log = if (logarithmic) "y" else "",
            xlab = "Years", ylab = if (logarithmic) "Population + 1" else "Population", main = "Runs (at most 100) and the median")
    q <- apply(N, 2, quantile, probs = c(0.05, 0.5, 0.95))
    if (logarithmic) q <- q + 1
    lines(years, q[2, ], lwd = 3, col = "#C44E52")
    lines(years, q[1, ], lwd = 2, lty = 2, col = "#C44E52")
    lines(years, q[3, ], lwd = 2, lty = 2, col = "#C44E52")
    legend("topright", c("Median", "5 % and 95 %"), lwd = c(3, 2), lty = c(1, 2), col = "#C44E52", bty = "n")
  })

  output$canvasRisk <- renderPlot({
    N <- runs()
    risk <- colMeans(N == 0)
    plot(0:(ncol(N) - 1), risk, type = "s", lwd = 2, col = "#C44E52", ylim = c(0, 1),
         xlab = "Years", ylab = "Risk of extinction", main = "Extinct runs over time")
    abline(h = c(0.05, 0.5), lty = 3, col = "grey50")
  })

  output$outSummary <- renderText({
    N <- runs()
    extinct <- N[, ncol(N)] == 0
    first <- apply(N[extinct, , drop = FALSE], 1, function(x) which(x == 0)[1] - 1)
    sprintf("Risk of extinction in %d years: %.1f %% (%d of %d runs)%s. Equilibrium without catastrophes: %.0f individuals.",
            ncol(N) - 1, 100 * mean(extinct), sum(extinct), nrow(N),
            if (any(extinct)) sprintf("; the extinct runs died out after %.1f years on average", mean(first)) else "",
            max(0, input$spinnerK * (1 - input$scaleDeath / input$scaleBirth)))
  })

  output$tableQuantiles <- renderTable({
    N <- runs()
    years <- ncol(N) - 1
    at <- unique(pmin(years, c(10, 25, 50, 100, 250, 500, 1000)))
    at <- at[at <= years]
    data.frame(Year = as.integer(at),
               Q5 = apply(N[, at + 1, drop = FALSE], 2, quantile, 0.05),
               Median = apply(N[, at + 1, drop = FALSE], 2, median),
               Q95 = apply(N[, at + 1, drop = FALSE], 2, quantile, 0.95),
               Extinct = sprintf("%.1f %%", 100 * colMeans(N[, at + 1, drop = FALSE] == 0)),
               check.names = FALSE)
  }, digits = 0)
}
