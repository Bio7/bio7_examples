# ImageJ macros from R and the results table as data frame: the objects of the
# Blobs sample are measured with the particle analyzer of ImageJ and analysed
# with R.
#
#   bio7_ij_macro(code)          # an ImageJ macro (the value of its return statement)
#   bio7_ij_run(command, opts)   # one ImageJ command on the current image
#   df <- bio7_results_table()   # the results table of ImageJ as data frame
#   r <- bio7_roi()              # the coordinates of the current selection
#
# Needs the internet for the sample (else a test image is created).

bio7_ij_macro('
  run("Blobs (25K)");
  if (nImages == 0) {
    newImage("Blobs (test)", "8-bit black", 256, 254, 1);
    for (i = 0; i < 40; i++) {
      setColor(255);
      makeOval(random * 230, random * 230, 8 + random * 14, 8 + random * 14);
      fill();
    }
    run("Select None");
  }
')

# Threshold and particle analyzer (the measurements in the results table)
bio7_ij_run("Set Measurements...", "area mean centroid perimeter shape redirect=None decimal=3")
bio7_ij_macro('
  run("Duplicate...", "title=mask");
  setAutoThreshold("Default");
  run("Convert to Mask");
  run("Analyze Particles...", "size=10-Infinity display clear add");
')

df <- bio7_results_table()
cat("Objects:", nrow(df), "\n")
print(summary(df[, c("Area", "Perim.", "Circ.")]))

# Plots of the measurements (in the plot view of R)
op <- par(mfrow = c(1, 2))
hist(df$Area, breaks = 20, col = "steelblue", main = "Object area", xlab = "Area (pixels)")
plot(df$X, df$Y, cex = sqrt(df$Area) / 6, pch = 21, bg = "orange",
     ylim = rev(range(df$Y)), main = "Positions (size ~ area)", xlab = "X", ylab = "Y")
par(op)

# A linear model: perimeter and area
fit <- lm(Perim. ~ sqrt(Area), data = df)
print(summary(fit)$coefficients)

# The selection of the largest object as coordinates
largest <- which.max(df$Area) - 1
bio7_ij_macro(paste0('roiManager("select", ', largest, ');'))
r <- bio7_roi()
cat("Largest object:", length(r$x), "outline points\n")
