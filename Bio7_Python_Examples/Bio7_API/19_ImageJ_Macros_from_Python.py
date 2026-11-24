# ImageJ macros from Python: the processing in the macro language of ImageJ,
# the analysis in Python (numpy), the values exchanged as macro return values,
# arguments and images.
#
#   bio7.ij_macro(code, arg="")       runs macro code, returns its return value (a string) or None
#   bio7.ij_macro_file(path, arg="")  runs a macro file (or relative to the ImageJ macros folder)
#   bio7.ij_run(command, options="")  runs one ImageJ command (as run() in a macro)
#
# A macro error raises a RuntimeError with the message and line of ImageJ.
#
# Requires: numpy

import numpy as np
import bio7

# A sample image of ImageJ, smoothed with a command
bio7.ij_macro('run("Blobs (25K)");')
bio7.ij_run("Gaussian Blur...", "sigma=1.5")

# A macro which measures the objects and returns the values as text
count = bio7.ij_macro('''
setAutoThreshold("Default");
run("Set Measurements...", "area mean centroid redirect=None decimal=3");
run("Analyze Particles...", "size=20-Infinity exclude clear");
resetThreshold();
return "" + nResults;
''')
print("Objects found by ImageJ:", count)

# The areas of the results table as numpy array (a macro returns text: comma separated)
areas = bio7.ij_macro('''
s = "";
for (i = 0; i < nResults; i++)
    s = s + getResult("Area", i) + ",";
return s;
''')
areas = np.array([float(v) for v in areas.split(",") if v])
print("Area: mean %.1f, median %.1f, sd %.1f" % (areas.mean(), np.median(areas), areas.std(ddof=1)))

# An argument for a macro: the objects larger than the median are kept
large = bio7.ij_macro('''
min = parseFloat(getArgument());
// The threshold again (the first macro removed it with resetThreshold)
blobs = getImageID();
setAutoThreshold("Default");
run("Analyze Particles...", "size=" + min + "-Infinity show=Masks exclude");
mask = getImageID();
selectImage(blobs);
resetThreshold();
selectImage(mask);
rename("Large objects");
return getTitle();
''', str(np.median(areas)))
print("New image:", large)

# The mask of ImageJ in Python (numpy), changed and shown again in ImageJ
mask = bio7.image("Large objects") > 0
from scipy import ndimage as ndi
distance = ndi.distance_transform_edt(mask)
bio7.show_image(distance.astype(np.float32), "Distance map (Python)")

# The result of Python in a macro: the maximum distance as argument
bio7.ij_macro('''
selectImage("Distance map (Python)");
run("Fire");
setMinAndMax(0, parseFloat(getArgument()));
''', str(float(distance.max())))

# Errors of a macro are Python exceptions
try:
    bio7.ij_macro('x = notAFunction(1);')
except RuntimeError as e:
    print("Caught:", e)
