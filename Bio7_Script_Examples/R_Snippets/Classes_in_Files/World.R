# The grid: the grass of each cell and the animals (needs Rabbit.R and Fox.R).

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
