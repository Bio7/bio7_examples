# A fox: an Animal which eats one rabbit of its cell (needs Animal.R).

Fox <- setRefClass("Fox",
  contains = "Animal",
  methods = list(
    maxAge = function() 60,
    moveCost = function() 1,
    birthEnergy = function() 30,
    birthProbability = function() 0.08,

    eat = function(world) {
      rabbit <- world$rabbitAt(x, y)
      if (!is.null(rabbit)) {
        rabbit$energy <- 0
        energy <<- energy + 20
      }
    }
  )
)
