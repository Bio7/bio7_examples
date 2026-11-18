# The Java API of Bio7, ImageJ and Java from R: classes with J(), static methods
# and fields, objects and their methods with $. Java objects are proxies in R;
# numbers, strings and arrays are converted.
#
#   IJ <- J("ij.IJ")                      # a Java class
#   imp <- IJ$getImage()                  # static methods; a Java object (proxy)
#   imp$getTitle()                        # methods of objects
#   roi <- J("ij.gui.OvalRoi")$new(10, 10, 50, 50)   # constructors with $new()
#   as.list(collection)                   # the elements of a Java collection
#   bio7_java_ui(obj)$method()            # in the UI thread (SWT widgets)

IJ <- J("ij.IJ")

# A new ImageJ image from R values (FloatProcessor: width, height, pixels row by row)
w <- 300; h <- 200
xy <- expand.grid(x = seq_len(w), y = seq_len(h))
z <- sin(xy$x / 20) * cos(xy$y / 15) * 100
fp <- J("ij.process.FloatProcessor")$new(w, h, z)
imp <- J("ij.ImagePlus")$new("Waves (from R)", fp)
imp$show()
cat("Created:", imp$getTitle(), imp$getWidth(), "x", imp$getHeight(), "\n")

# The pixels back to R: a Java array as vector (row by row) -> matrix
px <- imp$getProcessor()$getPixels()
m <- matrix(px, nrow = h, byrow = TRUE)
image(t(m)[, h:1], col = hcl.colors(64), main = "The pixels in R", axes = FALSE)

# The statistics of ImageJ
st <- imp$getStatistics()
cat("ImageJ statistics: mean", st$mean, " min", st$min, " max", st$max, "\n")

# A selection (ROI) and an overlay with Java objects
roi <- J("ij.gui.OvalRoi")$new(100, 50, 100, 100)
imp$setRoi(roi)
cat("Mean in the oval:", imp$getStatistics()$mean, "\n")
overlay <- J("ij.gui.Overlay")$new()
for (i in 0:4) {
  r <- J("ij.gui.Roi")$new(10 + i * 55, 10, 45, 30)
  r$setStrokeColor(J("java.awt.Color")$ORANGE)
  overlay$add(r)
}
imp$setOverlay(overlay)
imp$updateAndDraw()

# The Java standard library: collections, dates, strings
lst <- J("java.util.ArrayList")$new()
for (s in c("Bio7", "R", "ImageJ")) lst$add(s)
print(unlist(as.list(lst)))
print(J("java.time.LocalDate")$now()$plusDays(30)$toString())
# A Java object as argument: the format without the locale of the system
print(J("java.lang.String")$format(J("java.util.Locale")$ROOT, "%.3f", pi))

# The methods and fields of an object (as the completion after $)
print(head(.DollarNames(imp, "^get"), 20))
