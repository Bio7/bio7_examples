# Step 1 of the flow Sea_Turtle.flow: the stage matrix of the loggerhead sea turtle
# (Caretta caretta) and its analysis in R.
#
# The 7 stages and the yearly rates as commonly reproduced from Crouse, Crowder and
# Caswell (1987, Ecology 68: 1412-1423): eggs/hatchlings, small juveniles, large juveniles,
# subadults, novice breeders, first-year remigrants, mature breeders. The columns are the
# stage of this year, the rows the stage of the next year (the first row: the eggs).
# Please check the values against the original publication for a scientific use.
#
# - the growth rate lambda (the dominant eigenvalue) and the stable stage distribution
# - the sensitivities and elasticities (de Kroon et al. 1986): which rates change lambda most?
#   The elasticities of the juvenile survival are large, those of the eggs small: the
#   argument of the study for turtle excluder devices instead of nest protection alone.
#
# The next step (Java) projects the matrix "turtleMatrix" from the stable stage
# distribution "turtleStableStage"; the elasticities "turtleElasticity" are plotted in Python.

stages <- c("Eggs", "Small juveniles", "Large juveniles", "Subadults", "Novice breeders",
            "Remigrants", "Mature breeders")
turtleMatrix <- matrix(c(
  0,      0,      0,      0,      127,    4,      80,
  0.6747, 0.7370, 0,      0,      0,      0,      0,
  0,      0.0486, 0.6610, 0,      0,      0,      0,
  0,      0,      0.0147, 0.6907, 0,      0,      0,
  0,      0,      0,      0.0518, 0,      0,      0,
  0,      0,      0,      0,      0.8091, 0,      0,
  0,      0,      0,      0,      0,      0.8091, 0.8089
), nrow = 7, byrow = TRUE, dimnames = list(stages, stages))

right <- eigen(turtleMatrix)
i <- which.max(Re(right$values))
lambda <- Re(right$values[i])
w <- Re(right$vectors[, i]); w <- w / sum(w)
left <- eigen(t(turtleMatrix))
v <- Re(left$vectors[, which.max(Re(left$values))])
sensitivity <- outer(v, w) / sum(v * w)
elasticity <- sensitivity * turtleMatrix / lambda

turtleStableStage <- w
# The elasticities of the fecundities, the stasis (staying) and the growth (to the next stage)
turtleElasticity <- data.frame(
  stage = factor(stages, levels = stages),
  fecundity = elasticity[1, ],
  stasis = diag(elasticity),
  growth = c(sapply(1:6, function(j) elasticity[j + 1, j]), 0)
)
turtleElasticity$stasis[1] <- 0
turtleLambda <- lambda

cat(sprintf("Growth rate lambda = %.3f (%.1f %% per year)\n", lambda, 100 * (lambda - 1)))
cat("Stable stage distribution:\n"); print(round(setNames(w, stages), 3))
cat("Elasticities (sum 1):\n"); print(round(turtleElasticity[, -1], 3))
