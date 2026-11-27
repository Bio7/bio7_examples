# Step after the loop: the evaluation of the adaptive management (deerHistory: one row per
# year with the true population N, the survey estimate, the decision and the harvest).
#
# - the years in the target range and the decisions taken
# - the error of the survey estimate: the mean relative error (bias) and the share of the
#   years where the 90 % interval contains the true population
# - the browsing damage as a function of the density: a binomial glm (share of the young
#   trees browsed) and the density with a damage below 40 %
# - the harvest before and after the return of the wolves
#
# The results for the plots (Python): deerHistory and the list deerResult.

if (nrow(deerHistory) == 0) stop("Run the flow from the start: no years recorded.")

h <- deerHistory
inRange <- h$N >= deerPar$lower & h$N <= deerPar$upper
covered <- h$N >= h$est_lower & h$N <= h$est_upper

# Browsing: share of 200 young trees per plot (binomial) ~ density (deer per km2)
h$density <- h$N / deerPar$area
browsed <- round(200 * h$damage)
damageModel <- suppressWarnings(glm(cbind(browsed, 200 - browsed) ~ density, family = binomial, data = h))
co <- coef(damageModel)
damage40 <- (qlogis(0.4) - co[1]) / co[2]

deerResult <- list(
  years_in_range = sum(inRange),
  share_in_range = mean(inRange),
  later_in_range = mean(inRange[h$year > 10]),
  interval_coverage = mean(covered),
  bias = mean(h$estimate / h$N - 1),
  total_harvest = sum(h$harvest),
  harvest_before_wolves = mean(h$harvest[!h$wolves]),
  harvest_with_wolves = mean(h$harvest[h$wolves]),
  mean_damage = mean(h$damage),
  density_damage40 = unname(damage40)
)

cat("\nAdaptive management of", nrow(h), "years:\n")
print(table(decision = h$action))
cat(sprintf("Population in the target range (%d-%d): %d of %d years (%.0f %% after year 10)\n",
            deerPar$lower, deerPar$upper, sum(inRange), nrow(h), 100 * deerResult$later_in_range))
cat(sprintf("Survey estimate: mean error %+.0f %%, the 90 %% interval contains the true population in %.0f %% of the years\n",
            100 * deerResult$bias, 100 * deerResult$interval_coverage))
cat(sprintf("Deer shot per year: %.1f before the wolves, %.1f with the wolves (from year %d)\n",
            deerResult$harvest_before_wolves, deerResult$harvest_with_wolves, deerPar$wolvesFrom))
cat(sprintf("Browsing damage below 40 %% up to %.1f deer per km2 (target %.1f); total harvest %d deer\n",
            damage40, deerPar$target / deerPar$area, deerResult$total_harvest))
