# R and the embedded Python of Bio7: data frames and matrices are exchanged
# directly (in memory), Python code runs in the workspace of the Python console.
#
#   bio7_to_python("name", value)   # a data frame as pandas DataFrame, a vector or matrix as numpy array
#   x <- bio7_python("code")        # the value of the last expression as R value
#
# Python is started if necessary (Preferences > Python). Requires numpy and pandas.

# A data frame to Python (pandas)
bio7_to_python("iris_df", iris)
print(bio7_python("type(iris_df).__name__"))

# pandas: the means of the species (a DataFrame back to R)
means <- bio7_python('
iris_df.groupby("Species").mean(numeric_only=True).reset_index()
')
print(means)

# A matrix to numpy: the singular values (numpy) compared with R
m <- matrix(rnorm(200 * 5), 200, 5) %*% diag(5:1)
bio7_to_python("m", m)
sv_py <- bio7_python("import numpy as np\nnp.linalg.svd(m, compute_uv=False)")
sv_r <- svd(m)$d
print(rbind(Python = sv_py, R = sv_r))
cat("Max. difference:", max(abs(sv_py - sv_r)), "\n")

# Several statements: the value of the last expression (a dict as named list)
res <- bio7_python('
import numpy as np
x = np.linspace(0, 2 * np.pi, 50)
{"x": x, "sin": np.sin(x), "n": len(x)}
')
plot(res$x, res$sin, type = "l", lwd = 2, col = "darkgreen", main = paste("From Python:", res$n, "values"))
