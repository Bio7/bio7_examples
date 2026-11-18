# ImageJ images in R with the Bio7 API: the current image as matrix, processed
# with R and shown again in ImageJ.
#
#   img <- bio7_image()                 # matrix (height x width); a stack: array (slices, height, width)
#   bio7_show_image(img, "Title")       # a matrix (or array) as new ImageJ image
#   bio7_show_image(lab, "Labels", range = "auto")   # display range of the values
#
# Open an image in ImageJ (e.g., File > Open Samples > Blobs). Without an open
# image a test image is created with an ImageJ macro.

if (as.integer(bio7_ij_macro('return "" + nImages;')) == 0) {
  bio7_ij_macro('newImage("Test", "8-bit noise", 256, 256, 1); run("Gaussian Blur...", "sigma=4");')
}

img <- bio7_image()
cat("Image:", nrow(img), "x", ncol(img), "pixels, range", range(img), "\n")

# A smoothing with a 5 x 5 mean filter (R loops: some seconds for large images)
smooth <- function(m, k = 2) {
  out <- m
  for (i in seq_len(nrow(m))) {
    rows <- max(1, i - k):min(nrow(m), i + k)
    for (j in seq_len(ncol(m))) {
      out[i, j] <- mean(m[rows, max(1, j - k):min(ncol(m), j + k)])
    }
  }
  out
}
sm <- smooth(img)
bio7_show_image(sm, "Smoothed (R)")

# The difference: details of the image (contrast of its range)
bio7_show_image(img - sm, "Details (R)", range = "auto")

# A threshold with R: a binary image and the distribution of the values
thr <- mean(img) + sd(img)
bio7_show_image((img > thr) * 255, "Threshold (R)")
hist(img, breaks = 64, main = "Pixel values", xlab = "Value", col = "grey")
abline(v = thr, col = "red", lwd = 2)
