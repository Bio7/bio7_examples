# A rabbit: an Animal which eats the grass of its cell (needs Animal.R).

Rabbit <- setRefClass("Rabbit",
  contains = "Animal",
  methods = list(
    maxAge = function() 30,
    birthProbability = function() 0.2,

    eat = function(world) {
      energy <<- energy + world$graze(x, y)
    }
  )
)
