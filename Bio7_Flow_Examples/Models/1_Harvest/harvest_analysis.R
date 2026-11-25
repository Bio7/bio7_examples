# Step 2 of the flow Harvest.flow: the runs of the harvest model (Java, the data frame
# "harvest": rate, run, catch, stock, collapsed) analysed in R.
#
# - the mean total catch of 50 years for each harvest rate and the rate with the highest catch
#   (the maximum sustainable yield of the noisy population), compared with r/2 without noise
# - the risk of a collapse as a function of the harvest rate: a logistic regression (glm)
#   and the highest rate with a risk below 10 %
#
# The results for the next step (Python): the data frame "harvestSummary" and the list
# "harvestResult".

if (!exists("harvest")) stop("Run the flow from the start: the data frame 'harvest' is created by the Java step.")

harvestSummary <- aggregate(cbind(catch, collapsed) ~ rate, data = harvest, FUN = mean)
names(harvestSummary)[names(harvestSummary) == "collapsed"] <- "collapse_share"

collapseModel <- suppressWarnings(glm(collapsed ~ rate, family = binomial, data = harvest))
harvestSummary$collapse_risk <- predict(collapseModel, newdata = harvestSummary, type = "response")

best <- harvestSummary$rate[which.max(harvestSummary$catch)]
safe <- harvestSummary$rate[harvestSummary$collapse_risk < 0.1]
harvestResult <- list(
  best_rate = best,
  msy_rate_without_noise = 0.8 / 2,
  safe_rate = if (length(safe)) max(safe) else NA,
  slope = unname(coef(collapseModel)["rate"])
)

cat("Harvest rate with the highest mean catch:", best, "(without noise: r/2 =", 0.8 / 2, ")\n")
cat("Highest harvest rate with a collapse risk below 10 %:", harvestResult$safe_rate, "\n")
print(summary(collapseModel)$coefficients)
