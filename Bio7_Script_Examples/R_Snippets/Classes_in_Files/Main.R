# The predator-prey model of Classes_Predator_Prey.R with one class per file
# (Reference Classes). Run this file (Main.R): it sources the class files of
# its folder (the base class first), then starts the main class.
#
#   Animal.R      Animal      the base class
#   Rabbit.R      Rabbit      an Animal which eats grass
#   Fox.R         Fox         an Animal which hunts rabbits
#   World.R       World       the grid with the grass and the animals
#   Statistics.R  Statistics  the counts, the summary and the plot
#   Main.R        Main        the main class (this file)

# The folder of this file (source() knows the file), else the working directory
classFolder <- local({
  folder <- getwd()
  for (i in rev(seq_len(sys.nframe()))) {
    f <- sys.frame(i)$ofile
    if (!is.null(f)) {
      folder <- dirname(normalizePath(f))
      break
    }
  }
  folder
})

# The classes in the order of their dependencies (a subclass after its base class)
for (f in c("Animal.R", "Rabbit.R", "Fox.R", "World.R", "Statistics.R")) {
  source(file.path(classFolder, f))
}

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

main <- Main$new()
main$run()
