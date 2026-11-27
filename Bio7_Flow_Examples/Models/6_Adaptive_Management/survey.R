# Step 3 of the flow Adaptive_Management.flow (in the loop): the population estimated from
# the survey counts. Management doesn't know the true population (deerThisYear$N), only
# the count of the survey (a share pDetect of the deer is counted).
#
# - the estimate of this year: count / pDetect with a 90 % interval (Poisson count)
# - the trend of the last 5 years: a log-linear regression of the counts (growth rate)
# - the decision of the next two flow components (the decision shapes read deerFlow):
#     tooMany: the lower bound of the interval is above the upper limit (more hunting)
#     tooFew:  the upper bound of the interval is below the lower limit (no hunting)

if (!exists("deerThisYear")) stop("Run the flow from the start: deerThisYear is created by the Java step.")

count <- deerThisYear$count
estimate <- count / deerPar$pDetect
ci <- qchisq(c(0.05, 0.95), df = 2 * c(count, count + 1)) / 2 / deerPar$pDetect

counts <- c(deerHistory$count, count)
recent <- tail(counts, 5)
growth <- if (length(recent) >= 3) unname(coef(lm(log(recent + 1) ~ seq_along(recent)))[2]) else 0

deerAssessment <- list(estimate = estimate, lower = ci[1], upper = ci[2], growth = growth)
deerFlow <- list(
  tooMany = tolower(ci[1] > deerPar$upper),
  tooFew = tolower(ci[2] < deerPar$lower)
)

cat(sprintf("  Survey: %d deer counted, estimate %.0f (%.0f-%.0f), growth %+.2f per year -> %s\n",
            count, estimate, ci[1], ci[2], growth,
            if (deerFlow$tooMany == "true") "too many" else if (deerFlow$tooFew == "true") "too few" else "in the target range"))
