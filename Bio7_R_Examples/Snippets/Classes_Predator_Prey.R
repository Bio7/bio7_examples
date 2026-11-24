# An object-oriented R program with several classes and one main class
# (Reference Classes, setRefClass, part of R): a predator-prey model (rabbits,
# foxes and grass on a grid). The classes are shown in the Outline view; set
# breakpoints in the methods to follow the simulation in the debugger.
#
#   Animal      the base class: position, energy, age, moving, dying
#   Rabbit      an Animal which eats grass
#   Fox         an Animal which hunts rabbits
#   World       the grid with the grass and the animals (one time step)
#   Statistics  the counts of each step, a summary and the plot
#   Main        the main class: the parameters, the simulation, the results

# ---------------------------------------------------------------- Animal

Animal <- setRefClass("Animal",
  fields = list(x = "numeric", y = "numeric", energy = "numeric", age = "numeric"),
  methods = list(
    # The constants of the class (overridden by the subclasses)
    maxAge = function() 50,
    moveCost = function() 1,
    birthEnergy = function() 20,
    birthProbability = function() 0.1,

    move = function(world) {
      "A random step to a neighbour cell (the world is a torus)."
      d <- list(c(1, 0), c(-1, 0), c(0, 1), c(0, -1))[[sample.int(4, 1)]]
      x <<- (x + d[1] - 1) %% world$width + 1
      y <<- (y + d[2] - 1) %% world$height + 1
      energy <<- energy - moveCost()
      age <<- age + 1
    },

    isAlive = function() {
      energy > 0 && age < maxAge()
    },

    offspring = function() {
      "A new animal of the same class (half of the energy), or NULL."
      if (energy > birthEnergy() && runif(1) < birthProbability()) {
        energy <<- energy / 2
        return(getRefClass(class(.self))$new(x = x, y = y, energy = energy, age = 0))
      }
      NULL
    },

    eat = function(world) {
      "Overridden by the subclasses."
    },

    show = function() {
      cat(class(.self), "(x = ", x, ", y = ", y, ", energy = ", round(energy, 1), ", age = ", age, ")\n", sep = "")
    }
  )
)

# ---------------------------------------------------------------- Rabbit

Rabbit <- setRefClass("Rabbit",
  contains = "Animal",
  methods = list(
    maxAge = function() 30,
    birthProbability = function() 0.2,

    eat = function(world) {
      "A rabbit eats the grass of its cell."
      energy <<- energy + world$graze(x, y)
    }
  )
)

# ---------------------------------------------------------------- Fox

Fox <- setRefClass("Fox",
  contains = "Animal",
  methods = list(
    maxAge = function() 60,
    moveCost = function() 1,
    birthEnergy = function() 30,
    birthProbability = function() 0.08,

    eat = function(world) {
      "A fox eats one rabbit of its cell."
      rabbit <- world$rabbitAt(x, y)
      if (!is.null(rabbit)) {
        rabbit$energy <- 0
        energy <<- energy + 20
      }
    }
  )
)

# ---------------------------------------------------------------- World

World <- setRefClass("World",
  fields = list(width = "numeric", height = "numeric", grass = "matrix", grassGrowth = "numeric",
                grassMax = "numeric", animals = "list", rabbitsByCell = "environment"),
  methods = list(
    initialize = function(width = 30, height = 30, grassGrowth = 0.3, grassMax = 5, ...) {
      initFields(width = width, height = height, grassGrowth = grassGrowth, grassMax = grassMax,
                 grass = matrix(grassMax, height, width), animals = list(), ...)
    },

    populate = function(rabbits, foxes) {
      for (i in seq_len(rabbits)) {
        animals[[length(animals) + 1]] <<- Rabbit$new(x = sample.int(width, 1), y = sample.int(height, 1), energy = 10, age = 0)
      }
      for (i in seq_len(foxes)) {
        animals[[length(animals) + 1]] <<- Fox$new(x = sample.int(width, 1), y = sample.int(height, 1), energy = 20, age = 0)
      }
    },

    graze = function(x, y) {
      "The grass of a cell is eaten (its energy for the rabbit)."
      food <- min(grass[y, x], 4)
      grass[y, x] <<- grass[y, x] - food
      food
    },

    indexRabbits = function() {
      "The rabbits by cell (at the start of a step: a fast search of the foxes)."
      rabbitsByCell <<- new.env(hash = TRUE)
      for (a in animals) {
        if (is(a, "Rabbit")) {
          key <- paste(a$x, a$y)
          rabbitsByCell[[key]] <<- c(rabbitsByCell[[key]], a)
        }
      }
    },

    rabbitAt = function(x, y) {
      for (a in rabbitsByCell[[paste(x, y)]]) {
        if (a$energy > 0) {
          return(a)
        }
      }
      NULL
    },

    step = function() {
      "One time step: the grass grows, the animals move, eat and reproduce."
      grass <<- pmin(grass + grassGrowth, grassMax)
      indexRabbits()
      born <- list()
      for (a in animals[sample.int(length(animals))]) {
        if (!a$isAlive()) next
        a$move(.self)
        a$eat(.self)
        child <- a$offspring()
        if (!is.null(child)) born[[length(born) + 1]] <- child
      }
      animals <<- c(Filter(function(a) a$isAlive(), animals), born)
    },

    count = function(cls) {
      sum(vapply(animals, function(a) is(a, cls), logical(1)))
    }
  )
)

# ---------------------------------------------------------------- Statistics

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
                     xlab = "", ylab = "Animals", main = "Predator-prey model")
      lines(steps, foxes, col = "red", lwd = 2)
      legend("topright", legend = c("Rabbits", "Foxes"), col = c("darkgreen", "red"), lwd = 2, bty = "n")
      graphics::plot(steps, grass, type = "l", col = "olivedrab", lwd = 2, xlab = "Time step", ylab = "Grass")
    }
  )
)

# ---------------------------------------------------------------- Main

Main <- setRefClass("Main",
  fields = list(steps = "numeric", world = "World", statistics = "Statistics"),
  methods = list(
    initialize = function(width = 30, height = 30, rabbits = 200, foxes = 20, steps = 300, seed = 1, ...) {
      set.seed(seed)
      initFields(steps = steps, world = World$new(width = width, height = height), statistics = Statistics$new(), ...)
      world$populate(rabbits, foxes)
    },

    run = function() {
      "The simulation, the summary and the plot."
      statistics$record(world)
      for (s in seq_len(steps)) {
        world$step()
        statistics$record(world)
        if (world$count("Rabbit") == 0 && world$count("Fox") == 0) {
          cat("All animals died at step", s, "\n")
          break
        }
      }
      statistics$summary()
      statistics$plot()
    }
  )
)

# ---------------------------------------------------------------- Start

main <- Main$new()
main$run()
