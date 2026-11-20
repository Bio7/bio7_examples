#Example from the R help files for different type of classes. Visible in the outline view!

#S3 class

x <- 10
inherits(x, "a") #FALSE
class(x) <- c("a", "b")
inherits(x,"a") #TRUE
inherits(x, "a", TRUE) # 1
inherits(x, c("a", "b", "c"), TRUE) # 1 2 0

#S4 class

## A simple class with two slots
track <- setClass("track", slots = c(x="numeric", y="numeric"))
## an object from the class
t1 <- track(x = 1:10, y = 1:10 + rnorm(10))

## A class extending the previous, adding one more slot
trackCurve <- setClass("trackCurve",
		slots = c(smooth = "numeric"),
		contains = "track")
setClass("Person", representation(name = "character", age = "numeric"))
setClass("Employee", representation(boss = "Person"), contains = "Person")
alice <- new("Person", name = "Alice", age = 40)
john <- new("Employee", name = "John", age = 20, boss = alice)

#Reference class

## a simple editor for matrix objects.  Method  $edit() changes some
## range of values; method $undo() undoes the last edit.
mEdit <- setRefClass("mEdit",
      fields = list( data = "matrix",
        edits = "list"),
      methods = list(
     edit = function(i, j, value) {
       ## the following string documents the edit method
       'Replaces the range [i, j] of the
        object by value.
        '
         backup <-
             list(i, j, data[i,j])
         data[i,j] <<- value
         edits <<- c(edits, list(backup))
         invisible(value)
     },
     undo = function() {
       'Undoes the last edit() operation
        and update the edits field accordingly.
        '
         prev <- edits
         if(length(prev)) prev <- prev[[length(prev)]]
         else stop("No more edits to undo")
         edit(prev[[1]], prev[[2]], prev[[3]])
         ## trim the edits list
         length(edits) <<- length(edits) - 2
         invisible(prev)
     },
     show = function() {
       'Method for automatically printing matrix editors'
       cat("Reference matrix editor object of class",
          classLabel(class(.self)), "\n")
       cat("Data: \n")
       methods::show(data)
       cat("Undo list is of length", length(edits), "\n")
     }
     ))

xMat <- matrix(1:12,4,3)
xx <- mEdit(data = xMat)
xx$edit(2, 2, 0)
xx
xx$undo()
mEdit$help("undo")
stopifnot(all.equal(xx$data, xMat))


#R6 class

library(R6)
Person <- R6Class("Person",
  public = list(
    name = NA,
    hair = NA,
    initialize = function(name, hair) {
      if (!missing(name)) self$name <- name
      if (!missing(hair)) self$hair <- hair
      self$greet()
    },
    set_hair = function(val) {
      self$hair <- val
    },
    greet = function() {
      cat(paste0("Hello, my name is ", self$name, ".\n"))
    }
  )
)

ann <- Person$new("Ann", "black")
ann$set_hair("red")

#S7 class

## S7 (package S7, https://rconsortium.github.io/S7/) is the newer class
## system of the R Consortium: classes with typed properties and a validator,
## generics and methods. Shown in the outline view: the class with its
## properties and the methods ("speak(Dog)") in the class.
## Install it with install.packages("S7").
library(S7)

## A class with two typed properties; the validator checks the values
## (it returns a message for an invalid object, else NULL)
Dog <- new_class("Dog",
  properties = list(
    name = class_character,
    age = class_numeric
  ),
  validator = function(self) {
    if (length(self@age) == 1 && self@age < 0) "@age must be >= 0"
  }
)

## A subclass with one more property
ServiceDog <- new_class("ServiceDog", parent = Dog,
  properties = list(
    task = class_character
  )
)

## A generic and its methods (the method of the subclass calls the one of
## the parent class with super())
speak <- new_generic("speak", "x")
method(speak, Dog) <- function(x) {
  paste(x@name, "says woof")
}
method(speak, ServiceDog) <- function(x) {
  paste(speak(super(x, to = Dog)), "and helps with", x@task)
}

rex <- Dog(name = "Rex", age = 3)
speak(rex)
buddy <- ServiceDog(name = "Buddy", age = 5, task = "guiding")
speak(buddy)
## The properties are accessed with @ (the validator checks a new value)
rex@age <- 4
try(rex@age <- -1) # Error: @age must be >= 0
