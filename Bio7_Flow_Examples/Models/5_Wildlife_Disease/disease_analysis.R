# Step 2 of the flow Wildlife_Disease.flow: the epidemics of the SIR model (Java) analysed
# in R (the data frame "epidemics": the final size, the peak and the duration of each run;
# "epidemicCurves": the infected per day).
#
# - the basic reproduction number R0 estimated from the early exponential growth of the
#   outbreaks without vaccination: growth rate g = (R0 - 1) * gamma, so R0 = 1 + g / gamma
# - the critical vaccination coverage 1 - 1/R0 (herd immunity), compared with the model
#   value beta / gamma
# - the probability of a major outbreak (more than 10 % infected) per coverage
#
# The results for the next step (Python): "outbreaks" and the list "diseaseResult".

if (!exists("epidemics")) stop("Run the flow from the start: the data frame 'epidemics' is created by the Java step.")

parameters <- as.list(diseaseParameters)

# The early growth: log(infected) over the first 10 days of the outbreaks without vaccination
early <- subset(epidemicCurves, coverage == 0 & day <= 10 & infected > 0)
majorRuns <- unique(subset(epidemics, coverage == 0 & final_size > 0.1)$run)
early <- subset(early, run %in% majorRuns)
growth <- coef(lm(log(infected) ~ day, data = early))["day"]
r0 <- 1 + unname(growth) / parameters$gamma

outbreaks <- aggregate(cbind(major = final_size > 0.1, final_size, peak, duration) ~ coverage,
                       data = epidemics, FUN = mean)
diseaseResult <- list(
  r0_estimated = r0,
  r0_model = parameters$beta / parameters$gamma,
  critical_coverage = 1 - 1 / r0,
  critical_coverage_model = 1 - parameters$gamma / parameters$beta
)
cat(sprintf("R0 estimated from the early growth: %.2f (model: beta / gamma = %.2f)\n", r0, diseaseResult$r0_model))
cat(sprintf("Critical vaccination coverage 1 - 1/R0: %.2f (model: %.2f)\n",
            diseaseResult$critical_coverage, diseaseResult$critical_coverage_model))
print(round(outbreaks, 3))
