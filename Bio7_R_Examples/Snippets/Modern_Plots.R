# 1. Define shared design system & helper function
palette_colors <- c("#2B5C8F", "#D95F02", "#1B9E77", "#7570B3")

apply_design_theme <- function(main_title, sub_title, caption_text = "Source: R built-in datasets") {
  par(
    mar = c(5, 4.5, 4.5, 2),
    family = "sans",
    las = 1,
    bty = "n",
    fg = "gray30",
    col.axis = "gray30",
    col.lab = "gray20",
    bg = "white"
  )
  # Note: Title & caption are drawn after plot initialization via add_titles()
  assign(".plot_meta", list(main = main_title, sub = sub_title, cap = caption_text), envir = .GlobalEnv)
}

add_titles <- function() {
  meta <- get(".plot_meta", envir = .GlobalEnv)
  mtext(meta$main, side = 3, line = 2.5, adj = 0, cex = 1.3, font = 2, col = "gray10")
  mtext(meta$sub,  side = 3, line = 1.2, adj = 0, cex = 0.9, col = "gray40")
  mtext(meta$cap,  side = 1, line = 3.8, adj = 1, cex = 0.75, col = "gray50")
}

# Save original settings to restore when finished
old_par <- par(no.readonly = TRUE)
on.exit(par(old_par))

# Create a 2x2 grid on the default plot device to view all 4 charts together
# (Comment out the line below if you want each chart to render full-screen one by one)
par(mfrow = c(2, 2))


# ==============================================================================
# PLOT 1: SCATTER PLOT WITH LINEAR TRENDLINES
# ==============================================================================
data("mtcars")
mtcars$cyl <- factor(mtcars$cyl, levels = c(4, 6, 8))
pt_cols <- setNames(palette_colors[1:3], c("4", "6", "8"))

apply_design_theme(
  main_title = "1. Weight vs. Fuel Efficiency",
  sub_title  = "Scatter plot with group linear regressions",
  caption_text = "Source: mtcars dataset"
)

plot(mtcars$wt, mtcars$mpg, type = "n", xlim = c(1.5, 5.5), ylim = c(10, 35),
     xlab = "Weight (1,000 lbs)", ylab = "Miles per Gallon", font.lab = 2)
grid(col = "gray90", lty = "solid", lwd = 0.8)

for (lvl in levels(mtcars$cyl)) {
  sub_df <- subset(mtcars, cyl == lvl)
  mod <- lm(mpg ~ wt, data = sub_df)
  x_seq <- seq(min(sub_df$wt), max(sub_df$wt), length.out = 50)
  lines(x_seq, predict(mod, newdata = data.frame(wt = x_seq)), col = pt_cols[lvl], lwd = 2)
}

points(mtcars$wt, mtcars$mpg, pch = 21, bg = adjustcolor(pt_cols[as.character(mtcars$cyl)], 0.85),
       col = "white", lwd = 1.2, cex = 1.4)
legend("topright", legend = paste(c(4, 6, 8), "Cylinders"), pch = 21,
       pt.bg = pt_cols, col = "white", pt.cex = 1.3, bty = "n", cex = 0.85)
add_titles()


# ==============================================================================
# PLOT 2: BAR CHART WITH VALUE LABELS
# ==============================================================================
avg_mpg <- tapply(mtcars$mpg, mtcars$cyl, mean)

apply_design_theme(
  main_title = "2. Average MPG by Engine Size",
  sub_title  = "Mean fuel efficiency across cylinder groups",
  caption_text = "Source: mtcars dataset"
)

# Initialize empty barplot to place grid behind bars
bp <- barplot(avg_mpg, ylim = c(0, 32), col = NA, border = NA, axes = FALSE, names.arg = FALSE)
grid(nx = NA, ny = NULL, col = "gray90", lty = "solid", lwd = 0.8)

# Draw styled bars over grid
barplot(avg_mpg, add = TRUE, col = palette_colors[1:3], border = "white",
        names.arg = c("4 Cyl", "6 Cyl", "8 Cyl"), ylab = "Mean MPG", font.lab = 2, space = 0.35)

# Add direct data labels above bars
text(x = bp, y = avg_mpg + 1.5, labels = sprintf("%.1f", avg_mpg),
     font = 2, cex = 0.9, col = "gray20")
add_titles()


# ==============================================================================
# PLOT 3: MULTI-SERIES LINE CHART (TIME SERIES)
# ==============================================================================
data("EuStockMarkets")
# Extract first 100 trading days and normalize to index = 100 at Day 1
stocks <- EuStockMarkets[1:100, c("DAX", "SMI", "CAC")]
stocks_norm <- sweep(stocks, 2, stocks[1, ], "/") * 100
days <- 1:100

apply_design_theme(
  main_title = "3. European Market Performance",
  sub_title  = "Normalized index growth over first 100 trading days",
  caption_text = "Source: EuStockMarkets dataset"
)

plot(days, stocks_norm[, 1], type = "n", ylim = c(95, 115),
     xlab = "Trading Day", ylab = "Indexed Value (Day 1 = 100)", font.lab = 2)
grid(col = "gray90", lty = "solid", lwd = 0.8)

for (i in 1:3) {
  lines(days, stocks_norm[, i], col = palette_colors[i], lwd = 2.2)
}

legend("topleft", legend = colnames(stocks_norm), col = palette_colors[1:3],
       lwd = 2.2, bty = "n", cex = 0.85, horiz = TRUE)
add_titles()


# ==============================================================================
# PLOT 4: STYLED BOXPLOT WITH JITTERED DATA POINTS
# ==============================================================================
data("iris")

apply_design_theme(
  main_title = "4. Sepal Length Distribution",
  sub_title  = "Boxplot summary overlaid with individual observations",
  caption_text = "Source: iris dataset"
)

boxplot(Sepal.Length ~ Species, data = iris, ylim = c(4, 8.5),
        col = adjustcolor(palette_colors[1:3], alpha.f = 0.25),
        border = palette_colors[1:3], boxwex = 0.45, outline = FALSE,
        xlab = "Iris Species", ylab = "Sepal Length (cm)", font.lab = 2,
        names = c("Setosa", "Versicolor", "Virginica"))
grid(nx = NA, ny = NULL, col = "gray90", lty = "solid", lwd = 0.8)

# Overlay jittered points for distribution transparency
set.seed(42)
for (i in 1:3) {
  sp_vals <- iris$Sepal.Length[as.numeric(iris$Species) == i]
  jitter_x <- rep(i, length(sp_vals)) + runif(length(sp_vals), -0.12, 0.12)
  points(jitter_x, sp_vals, pch = 21, bg = palette_colors[i], col = "white", cex = 1.1, lwd = 0.8)
}
add_titles()
