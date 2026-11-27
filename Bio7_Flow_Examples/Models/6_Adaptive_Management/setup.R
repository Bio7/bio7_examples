# Step 1 of the flow Adaptive_Management.flow: the parameters and the start of the
# adaptive management of a red deer population in a forest of 25 km2.
#
# Each year of the loop (30 years) the flow
#   - simulates the population (Java: hunting, winter, births, the survey count),
#   - estimates the population from the survey (R),
#   - decides: too many deer (browsing damage), too few, or in the target range,
#   - sets the harvest quota of the next year (R, BeanShell or Python),
#   - records the year (R; the loop repeats the steps from the Java model).
# After the loop R evaluates the management and Python plots the 30 years.
# In year 15 wolves recolonise the forest: management has to adapt to the new predator.
#
# The objects of the flow: deerPar (parameters), deerAges (deer per age class, the
# "true" population which management doesn't know), deerQuota, deerYear, deerHistory.

deerPar <- list(
  area = 25,          # forest area (km2)
  K = 450,            # carrying capacity (deer)
  target = 150,       # target population (6 deer per km2)
  lower = 120,        # below: no hunting
  upper = 180,        # above: more hunting
  pDetect = 0.25,     # share of the deer counted in the survey (known from a calibration)
  wolvesFrom = 15,    # wolves recolonise the forest in this year (unknown to management)
  wolfPredation = 0.1, # share of the deer taken by the wolves per year
  years = 30,         # = loop count of the flow + 1
  seed = 7
)

# The start: an overabundant population (stable age distribution of about 320 deer)
ageShare <- c(0.22, 0.15, 0.12, 0.1, 0.09, 0.08, 0.06, 0.05, 0.04, 0.03, 0.02, 0.02, 0.01, 0.005, 0.005)
deerAges <- round(320 * ageShare / sum(ageShare))
deerQuota <- 40
deerYear <- 0

# The records of the years (the Java step adds deerThisYear, record.R the management)
deerHistory <- data.frame()
deerFlow <- list(tooMany = "false", tooFew = "false")

cat("Adaptive management: ", sum(deerAges), " deer at the start, target ", deerPar$target,
    " (", deerPar$lower, "-", deerPar$upper, "), ", deerPar$years, " years.\n", sep = "")
