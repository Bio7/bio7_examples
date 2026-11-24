# Template matching: finds all copies of several shapes (templates) in an
# image and selects them in ImageJ: the ROI Manager lists each match (name,
# number, score) in the color of its template, and all matches are one
# combined selection on the image (e.g., Analyze > Measure measures them all).
#
# A test image with circles, squares, triangles, crosses and rings is created
# with a template of the first four shapes (the rings are similar to the
# circles: they show how the threshold separates similar shapes).
#
# Own templates: select a shape in an ImageJ image with the rectangle tool and
# call template_matching.add_template("name") (or use the Java GUI:
# Bio7_Java_Examples/src/python/templatematching/TemplateMatching.java).
#
# Requires: opencv-python-headless, matplotlib

import sys

import matplotlib.pyplot as plt

sys.modules.pop("template_matching", None)   # the module of this folder, imported again after changes
import template_matching as tm  # noqa: E402

THRESHOLD = 0.7                  # the smallest score (0..1) of a match
METHOD = "TM_CCOEFF_NORMED"      # also TM_CCORR_NORMED, TM_SQDIFF_NORMED

n = tm.test_image()
print("Test image: %d shapes, templates: %s" % (n, ", ".join(tm.template_names())))

matches = tm.detect(THRESHOLD, METHOD)
counts = tm.select_in_imagej(matches)
for name in tm.template_names():
    print("  %-9s %d found" % (name, counts.get(name, 0)))
print("Not matched (rings, 7 in the image) or found twice: check the ROI Manager.")

# The templates and the score distribution of the matches
fig, axes = plt.subplots(1, len(tm.TEMPLATES) + 1, figsize=(11, 3), constrained_layout=True,
                         gridspec_kw={"width_ratios": [1] * len(tm.TEMPLATES) + [3]})
for ax, (name, t) in zip(axes, tm.TEMPLATES.items()):
    ax.imshow(t, cmap="gray")
    ax.set_title(name)
    ax.set_axis_off()
for i, name in enumerate(tm.template_names()):
    scores = [m[5] for m in matches if m[0] == name]
    axes[-1].scatter(scores, [i] * len(scores), color=[c / 255 for c in tm.COLORS[i]], edgecolor="k", s=40)
axes[-1].axvline(THRESHOLD, color="gray", ls="--")
axes[-1].set(yticks=range(len(tm.TEMPLATES)), yticklabels=tm.template_names(), xlabel="Score", title="Matches")
plt.show()
