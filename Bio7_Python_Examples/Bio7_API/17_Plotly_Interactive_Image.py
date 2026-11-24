# Interactive charts of an ImageJ image with plotly in the Browser view of Bio7:
# the intensities as a 3-d surface (rotate, zoom) and the measured objects as a
# scatter plot with hover information (label, area, position).
#
# Open an image in ImageJ (e.g., File > Open Samples > Blobs). Without an open
# image a sample is used.
#
# Requires: numpy, pandas, scipy, scikit-image, plotly

import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from scipy import ndimage as ndi
from skimage import filters, measure, segmentation, transform
import bio7

try:
    img = bio7.image()
except RuntimeError:
    from skimage import data
    img = data.coins()
    bio7.show_image(img, "Coins (sample)")

gray = (img if img.ndim == 2 else (img.mean(axis=-1) if img.shape[-1] == 3 else img[0])).astype("float64")

# The intensities as surface (reduced to at most 150 pixels per side for a fluid view)
scale = min(1.0, 150 / max(gray.shape))
small = transform.rescale(gray, scale, anti_aliasing=True, preserve_range=True)
surface = go.Figure(go.Surface(z=small, colorscale="Viridis"))
surface.update_layout(title="Intensity surface", scene=dict(zaxis_title="Intensity",
                                                            yaxis=dict(autorange="reversed")))
surface.show()

# The objects with their measurements (hover over a point)
smooth = filters.gaussian(gray, sigma=1)
mask = smooth > filters.threshold_otsu(smooth)
if mask.mean() > 0.5:
    mask = ~mask
mask = segmentation.clear_border(ndi.binary_opening(ndi.binary_fill_holes(mask), iterations=2))
labels = measure.label(mask)
objects = pd.DataFrame(measure.regionprops_table(
    labels, intensity_image=gray, properties=["label", "area", "eccentricity", "mean_intensity", "centroid"]))
objects = objects.rename(columns={"centroid-0": "y", "centroid-1": "x"})
objects = objects[objects["area"] >= 20]

fig = px.scatter(objects, x="area", y="mean_intensity", color="eccentricity", size="area",
                 hover_data={"label": True, "x": ":.0f", "y": ":.0f"},
                 title="Objects: area and mean intensity", color_continuous_scale="Turbo")
fig.show()

# The positions of the objects on the image
pos = px.imshow(gray, color_continuous_scale="gray", title="Object positions")
pos.add_trace(go.Scatter(x=objects["x"], y=objects["y"], mode="markers+text", text=objects["label"].astype(str),
                         textposition="top center", marker=dict(color="red", size=6), name="objects"))
pos.show()
print(len(objects), "objects; the charts are in the Browser view")
