# Step after the decisions (in the loop): the year with the estimate, the decision and the
# quota is added to deerHistory. The loop of the flow then repeats the year (Java model,
# survey, decision, management) until the years are done.

deerHistory <- rbind(deerHistory, data.frame(
  deerThisYear,
  estimate = deerAssessment$estimate,
  est_lower = deerAssessment$lower,
  est_upper = deerAssessment$upper,
  action = deerAction,
  quota = deerQuota
))
rm(deerThisYear)
