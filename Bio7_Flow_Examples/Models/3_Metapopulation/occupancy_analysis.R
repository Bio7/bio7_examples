# Step 3 of the flow Metapopulation.flow: the occupancy of the patches analysed in R.
#
# The Java step simulated the incidence function model (the data frame "patchOccupancy":
# the years each patch was occupied or empty, its area and connectivity; "occupancyTime":
# the share of occupied patches per year).
#
# - a binomial model of the incidence: occupied years ~ log(area) + log(connectivity);
#   larger and better connected patches are occupied more often (the two processes of a
#   metapopulation: extinction depends on the area, colonisation on the connectivity)
# - the share of occupied patches at equilibrium (the last 100 years of all runs)
#
# The results for the next step (Python): "patchOccupancy" with the fitted incidence and
# the list "metapopulationResult".

if (!exists("patchOccupancy")) stop("Run the flow from the start: the data frame 'patchOccupancy' is created by the Java step.")

incidenceModel <- glm(cbind(occupied, empty) ~ log(area) + log(connectivity + 1e-6),
                      family = binomial, data = patchOccupancy)
patchOccupancy$fitted <- fitted(incidenceModel)

equilibrium <- mean(occupancyTime$share[occupancyTime$year > 100])
metapopulationResult <- list(
  equilibrium = equilibrium,
  area_effect = unname(coef(incidenceModel)[2]),
  connectivity_effect = unname(coef(incidenceModel)[3]),
  deviance_explained = 1 - incidenceModel$deviance / incidenceModel$null.deviance
)
print(summary(incidenceModel)$coefficients)
cat(sprintf("Occupied patches at equilibrium: %.0f %%\n", 100 * equilibrium))
cat(sprintf("Deviance explained by area and connectivity: %.0f %%\n", 100 * metapopulationResult$deviance_explained))
