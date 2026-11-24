# HTML pages and URLs in the browser view of Bio7: an HTML report of an analysis
# written with R and shown with bio7_browse().
#
#   bio7_browse("https://...")     # a URL
#   bio7_browse("report.html")     # an HTML file

fit <- lm(mpg ~ wt + hp, data = mtcars)
co <- round(summary(fit)$coefficients, 4)

rows <- paste0("<tr><td>", rownames(co), "</td>",
               apply(co, 1, function(r) paste0("<td>", r, "</td>", collapse = "")), "</tr>", collapse = "\n")
png_file <- file.path(tempdir(), "mtcars.png")
png(png_file, width = 640, height = 420)
plot(mtcars$wt, mtcars$mpg, pch = 19, col = "steelblue", xlab = "Weight", ylab = "MPG", main = "mtcars")
abline(lm(mpg ~ wt, data = mtcars), col = "red", lwd = 2)
dev.off()

html <- paste0('<!doctype html><html><head><meta charset="utf-8"><title>Report</title>
<style>body{font-family:sans-serif;margin:24px} table{border-collapse:collapse}
td,th{border:1px solid #ccc;padding:4px 10px;text-align:right}</style></head><body>
<h2>Linear model: mpg ~ wt + hp</h2>
<table><tr><th></th><th>Estimate</th><th>Std. Error</th><th>t value</th><th>p</th></tr>
', rows, '</table>
<p>R squared: ', round(summary(fit)$r.squared, 3), '</p>
<img src="mtcars.png" width="640"></body></html>')
report <- file.path(tempdir(), "report.html")
writeLines(html, report)
bio7_browse(report)
