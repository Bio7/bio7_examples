# An experiment with the model of network_epidemic.R (batch runs without a display).
#
# The share of the infected people at the end for some infection rates and two networks,
# 5 repetitions each (the seeds 42 to 46: the same for all values). experiment() returns a
# data frame and shows its progress in Bio7 (bio7_progress: a job in the Progress view, its stop
# button interrupts the experiment); the settings of the model are restored afterwards. The same runs can be started
# in Bio7 with the button "Experiment..." of the settings of the model (the data frame
# "experiment" in R).

# The model: the script beside this one (it also opens the model in Bio7)
here <- getwd()
for (k in rev(seq_len(sys.nframe()))) {
  f <- tryCatch(sys.frame(k)$ofile, error = function(e) NULL)
  if (is.character(f)) { here <- dirname(f); break }
}
source(file.path(here, "network_epidemic.R"))

# the progress of the 50 runs: a job in the Progress view of Bio7 (its stop button interrupts the experiment)
d <- experiment(m, params = list(infection = c(0.01, 0.02, 0.03, 0.05, 0.08), network = c("Moving contacts", "Small world")),
                steps = 300, repetitions = 5, every = 300)

# the last row of each run (a run ends early when the epidemic is over: m$stop())
final <- d[!duplicated(d$run, fromLast = TRUE), ]
share <- aggregate(`Infected share` ~ infection + network, data = final, FUN = mean)
print(share)

# The final size of the epidemic over the infection rate (one line for each network)
nets <- unique(share$network)
plot(NA, xlim = range(share$infection), ylim = c(0, 1), xlab = "Infection per contact and step",
     ylab = "Share of the infected people (at the end)", main = "Network epidemic: 5 runs per value")
for (k in seq_along(nets)) {
  s <- share[share$network == nets[k], ]
  lines(s$infection, s$`Infected share`, type = "b", pch = 19, col = k + 1, lwd = 2)
  points(final$infection[final$network == nets[k]], final$`Infected share`[final$network == nets[k]], col = adjustcolor(k + 1, 0.35))
}
legend("bottomright", legend = nets, col = seq_along(nets) + 1, lwd = 2, pch = 19, bty = "n")
