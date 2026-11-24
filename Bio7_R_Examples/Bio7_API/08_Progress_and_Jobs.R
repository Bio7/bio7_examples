# The progress of R code in Bio7: jobs with a progress bar in the status bar and the
# Progress view (Window > Show View > Progress). The stop button of a job interrupts R
# (as the stop of the console).
#
#   bio7_progress(i, n, "text")          a progress bar for the running code
#   t <- bio7_task("text", total = n)    a named task (a job of its own):
#   t$update()                           one step (t$update(n, "text") for more)
#   t$done()                             the end of the task
#   bio7_progress_end()                  ends the job of bio7_progress
#
# A long call of a C function (e.g., one big lm()) ends before the interruption
# takes effect.

set.seed(1)

# 1. bio7_progress: the steps of a loop
images <- lapply(1:40, function(i) matrix(runif(256 * 256), 256))
means <- numeric(length(images))
for (i in seq_along(images)) {
  means[i] <- mean(stats::filter(as.vector(images[[i]]), rep(1 / 25, 25)), na.rm = TRUE)
  Sys.sleep(0.05)                                   # the work of a real analysis
  bio7_progress(i, length(images), sprintf("Filtering image %d of %d", i, length(images)))
}
cat("Filtered", length(images), "images\n")

# 2. A named task (a job of its own)
t <- bio7_task("Thresholding", total = length(images))
sizes <- numeric(length(images))
for (i in seq_along(images)) {
  sizes[i] <- sum(images[[i]] > 0.9)
  Sys.sleep(0.03)
  t$update()
}
t$done()
cat("Pixels above the threshold: mean", mean(sizes), "\n")

# 3. A task with a changing text
data <- rnorm(500, 10, 2)
t <- bio7_task("Bootstrap of the mean", total = 200)
estimates <- numeric(200)
for (b in 1:200) {
  estimates[b] <- mean(sample(data, replace = TRUE))
  if (b %% 50 == 0) t$update(text = sprintf("Bootstrap: %.3f so far", mean(estimates[1:b]))) else t$update()
  Sys.sleep(0.005)
}
t$done()
cat(sprintf("Bootstrap 95 %% interval: %.2f - %.2f\n", quantile(estimates, 0.025), quantile(estimates, 0.975)))

# 4. An unknown total: the job shows the count
k <- 0
while (k < 300) {
  k <- k + 1
  if (k %% 10 == 0) bio7_progress(k, text = "Searching")
  Sys.sleep(0.005)
}
bio7_progress_end()
cat("Done after", k, "iterations\n")

# 5. A long calculation: stop it with the stop button of the job
total <- tryCatch({
  s <- 0
  for (i in 1:400) {
    s <- s + sum(svd(matrix(runif(120 * 120), 120))$d)
    bio7_progress(i, 400, "Long calculation")
    Sys.sleep(0.01)
  }
  cat("Long calculation finished:", round(s, 1), "\n")
  s
}, interrupt = function(e) {
  bio7_progress_end()
  cat("Stopped by the user (stop button of the job)\n")
  NA
})
