# Step 2 of the flow Enrichment.flow: the paradox of enrichment analysed in R.
#
# The Java step simulated the Rosenzweig-MacArthur model for carrying capacities K
# (the data frame "enrichment": the minimum and maximum of prey and predator after the
# transient; "trajectories": the first 300 time units for K = 1.5 and K = 4).
#
# - stable equilibrium or limit cycle: the amplitude of the prey (max - min)
# - the K where the cycles start, compared with the Hopf bifurcation of the model:
#   K_hopf = (e + m h) / (a h (e - m h)), with the equilibrium prey N* = m / (a (e - m h))
# - the period of the cycles for K = 4 from the time between the peaks of the prey (after
#   150 time units: on the limit cycle)
#
# The results for the next step (Python): "enrichment" (with the column "state") and the
# list "enrichmentResult".

if (!exists("enrichment")) stop("Run the flow from the start: the data frame 'enrichment' is created by the Java step.")

p <- as.list(enrichmentParameters)
enrichment$amplitude <- enrichment$prey_max - enrichment$prey_min
enrichment$state <- ifelse(enrichment$amplitude > 0.01, "cycles", "stable")

nStar <- p$m / (p$a * (p$e - p$m * p$h))
kHopf <- (p$e + p$m * p$h) / (p$a * p$h * (p$e - p$m * p$h))
kCycles <- min(enrichment$K[enrichment$state == "cycles"])

prey <- trajectories[trajectories$K == 4 & trajectories$time > 150, ]
peaks <- which(diff(sign(diff(prey$prey))) == -2) + 1
period <- if (length(peaks) > 1) mean(diff(prey$time[peaks])) else NA

enrichmentResult <- list(n_star = nStar, k_hopf = kHopf, k_cycles = kCycles, period = period)
cat(sprintf("Equilibrium prey N* = %.3f\n", nStar))
cat(sprintf("Cycles from K = %.1f on (Hopf bifurcation of the model: K = %.2f)\n", kCycles, kHopf))
cat(sprintf("Period of the cycles at K = 4: %.1f time units\n", period))
print(table(enrichment$state))
