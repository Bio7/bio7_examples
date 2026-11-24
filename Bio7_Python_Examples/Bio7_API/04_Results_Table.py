# The ImageJ results table as pandas DataFrame.
#
# Measure something in ImageJ first, e.g., open "Blobs", threshold it
# (Image > Adjust > Threshold) and run Analyze > Analyze Particles with
# "Display results". Then run this script.
#
# Requires: pandas, matplotlib

import matplotlib.pyplot as plt
import bio7

rt = bio7.results_table()
print(rt.describe().round(2))

numeric = rt.select_dtypes("number").drop(columns=[" "], errors="ignore")
column = "Area" if "Area" in numeric else numeric.columns[0]

fig, axes = plt.subplots(1, 2, constrained_layout=True)
axes[0].hist(numeric[column], bins=20, color="#4C72B0", alpha=0.8)
axes[0].set(title=column, xlabel=column, ylabel="Count")
if "Circ." in numeric:
    axes[1].scatter(numeric[column], numeric["Circ."], color="#C44E52", alpha=0.7)
    axes[1].set(title="Shape", xlabel=column, ylabel="Circularity")
else:
    axes[1].boxplot(numeric[column])
    axes[1].set(title=column)
plt.show()

# The results back to R (if R runs): a data frame "particles"
try:
    bio7.to_r("particles", rt)
    print("Transferred to R:", bio7.r("dim(particles)"))
except RuntimeError as e:
    print("Not transferred to R:", e)
