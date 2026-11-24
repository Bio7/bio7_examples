# The counts of each time step, a summary and the plot.

Statistics <- setRefClass("Statistics",
  fields = list(rabbits = "numeric", foxes = "numeric", grass = "numeric"),
  methods = list(
    record = function(world) {
      rabbits <<- c(rabbits, world$count("Rabbit"))
      foxes <<- c(foxes, world$count("Fox"))
      grass <<- c(grass, sum(world$grass))
    },

    summary = function() {
      cat("Steps:", length(rabbits) - 1, "\n")
      cat("Rabbits: max", max(rabbits), ", last", tail(rabbits, 1), "\n")
      cat("Foxes:   max", max(foxes), ", last", tail(foxes, 1), "\n")
    },

    plot = function() {
      "The populations over the time (in the plot view of Bio7)."
      op <- par(mfrow = c(2, 1), mar = c(4, 4, 2, 1))
      on.exit(par(op))
      steps <- seq_along(rabbits) - 1
      graphics::plot(steps, rabbits, type = "l", col = "darkgreen", lwd = 2, ylim = c(0, max(rabbits, foxes)),
                     xlab = "", ylab = "Animals", main = "Predator-prey model (classes in files)")
      lines(steps, foxes, col = "red", lwd = 2)
      legend("topright", legend = c("Rabbits", "Foxes"), col = c("darkgreen", "red"), lwd = 2, bty = "n")
      graphics::plot(steps, grass, type = "l", col = "olivedrab", lwd = 2, xlab = "Time step", ylab = "Grass")
    }
  )
)
