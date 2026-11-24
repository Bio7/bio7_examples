# A sensitivity analysis of network_epidemic.R: which parameter decides the size of the epidemic?
#
# A Latin hypercube (package lhs) samples 40 parameter sets of the infection rate, the recovery
# rate and the contact distance. experiment(design = ...) runs each set 3 times (the seeds 42 to
# 44) without a display; the column point of the result is the row of the design. The analysis is
# done in R: the mean and the 95 % confidence interval of the infected share over the repetitions
# of each set, and the partial rank correlation coefficients (PRCC, package sensitivity) of the
# parameters. The packages are installed if they are missing.

# The model: the script beside this one (it also opens the model in Bio7)
here <- getwd()
for (k in rev(seq_len(sys.nframe()))) {
  f <- tryCatch(sys.frame(k)$ofile, error = function(e) NULL)
  if (is.character(f)) { here <- dirname(f); break }
}
source(file.path(here, "network_epidemic.R"))

for (p in c("lhs", "sensitivity")) if (!requireNamespace(p, quietly = TRUE)) install.packages(p)

# The design: a Latin hypercube in the ranges of the parameters (a row is a parameter set)
ranges <- list(infection = c(0.01, 0.1), recovery = c(0.02, 0.1), distance = c(2, 8))
set.seed(1)
u <- lhs::randomLHS(40, length(ranges))
design <- as.data.frame(mapply(function(r, k) r[1] + u[, k] * (r[2] - r[1]), ranges, seq_along(ranges)))

# 120 runs (the progress: a job in the Progress view of Bio7)
d <- experiment(m, design = design, steps = 300, repetitions = 3, every = 300)

# The last row of each run, then the mean and the 95 % confidence interval of each parameter set
final <- d[!duplicated(d$run, fromLast = TRUE), ]
ci <- function(v) {
  h <- if (length(v) > 1) qt(0.975, length(v) - 1) * sd(v) / sqrt(length(v)) else NA
  # (a share: the interval within 0 and 1)
  c(mean = mean(v), lower = max(0, mean(v) - h), upper = min(1, mean(v) + h))
}
stats <- do.call(rbind, tapply(final$`Infected share`, final$point, ci))
result <- cbind(design[as.integer(rownames(stats)), ], stats)
print(head(result[order(-result$mean), ], 10), digits = 3)

# The partial rank correlation of each parameter with the infected share (bootstrap intervals)
prcc <- sensitivity::pcc(result[, names(ranges)], result$mean, rank = TRUE, nboot = 200)
print(prcc)

# The infected share over each parameter (the mean and the interval of each set) and the PRCC
par(mfrow = c(2, 2), mar = c(4.5, 4.5, 2.5, 1))
for (n in names(ranges)) {
  plot(result[[n]], result$mean, pch = 19, col = "#2f6fed", ylim = c(0, 1.05),
       xlab = n, ylab = "Infected share (mean of 3 runs)", main = n)
  w <- which(result$upper > result$lower)   # the sets with different runs
  arrows(result[[n]][w], result$lower[w], result[[n]][w], result$upper[w], angle = 90, code = 3, length = 0.02, col = "#9aa0a6")
}
e <- prcc$PRCC
bp <- barplot(e$original, names.arg = rownames(e), ylim = c(-1, 1), col = ifelse(e$original > 0, "#e53935", "#2f6fed"),
              ylab = "PRCC", main = "Partial rank correlation")
arrows(bp, e$`min. c.i.`, bp, e$`max. c.i.`, angle = 90, code = 3, length = 0.05)
abline(h = 0)
