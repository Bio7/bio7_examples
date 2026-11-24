# The progress of Python code in Bio7: a running script is shown as job
# ("Python: <script>") in the status bar and the Progress view (Window > Show
# View > Progress) with its running time; the stop button of the job (or a
# double-click on the Python memory display of the status bar) interrupts it.
#
#   bio7.progress(i, n, "text")              a progress bar for the script
#   with bio7.task("text", total=n) as t:    a named task: t.update() per step
#   for x in bio7.task("text", items):       a task over an iterable
#   tqdm loops (also of libraries)           shown as jobs automatically
#
# A blocking call (e.g., a long time.sleep or one long C function) ends before
# the interruption takes effect.
#
# Requires: numpy (tqdm optional)

import time

import bio7
import numpy as np

rng = np.random.default_rng(1)

# 1. bio7.progress: the steps of a loop
images = [rng.random((256, 256)) for _ in range(40)]
means = []
for i, img in enumerate(images):
    means.append(np.convolve(img.ravel(), np.ones(25) / 25, mode="same").mean())
    time.sleep(0.05)                                  # the work of a real analysis
    bio7.progress(i + 1, len(images), "Filtering image %d of %d" % (i + 1, len(images)))
print("Filtered", len(images), "images")

# 2. A named task for an iterable
sizes = []
for img in bio7.task("Thresholding", images):
    sizes.append(int((img > 0.9).sum()))
    time.sleep(0.03)
print("Pixels above the threshold: mean", np.mean(sizes))

# 3. A task with explicit steps and changing text
with bio7.task("Bootstrap of the mean", total=200) as t:
    data = rng.normal(10, 2, 500)
    estimates = []
    for b in range(200):
        estimates.append(rng.choice(data, data.size).mean())
        if b % 50 == 0:
            t.update(text="Bootstrap: %.3f so far" % np.mean(estimates))
        else:
            t.update()
        time.sleep(0.005)
print("Bootstrap 95 %% interval: %.2f - %.2f" % tuple(np.percentile(estimates, [2.5, 97.5])))

# 4. tqdm (as used by many libraries) is shown as job, too
try:
    from tqdm import tqdm
    for _ in tqdm(range(30), desc="tqdm loop"):
        time.sleep(0.05)
except ImportError:
    print("tqdm is not installed (Python Packages): skipped")

# 5. A long calculation: stop it with the stop button of the job
try:
    total = 0.0
    for i in range(400):
        total += np.linalg.svd(rng.random((120, 120)), compute_uv=False).sum()
        time.sleep(0.01)
    print("Long calculation finished:", round(total, 1))
except KeyboardInterrupt:
    print("Stopped by the user (stop button of the job)")
