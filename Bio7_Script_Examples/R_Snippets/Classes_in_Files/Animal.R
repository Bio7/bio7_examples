# The base class of the animals: position, energy and age.

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
