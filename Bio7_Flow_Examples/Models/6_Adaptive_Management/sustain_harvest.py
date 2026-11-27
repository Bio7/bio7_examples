"""Decision "Too few deer?" = false (in the loop of Adaptive_Management.flow): the population
is in the target range. The quota of the next year is the expected yearly increase near the
target (the surplus production of the logistic growth, r N (1 - N/K)), corrected for the
trend of the counts. A Python step: it reads the assessment from R and sets the quota in R.
"""
import numpy as np

import bio7


def num(value):
    """A single number of R (R returns vectors: the first element)."""
    return float(np.asarray(value, dtype=float).ravel()[0])


estimate = num(bio7.r("deerAssessment$estimate"))
growth = num(bio7.r("deerAssessment$growth"))
k = num(bio7.r("deerPar$K"))
target = num(bio7.r("deerPar$target"))

r = 0.2  # the yearly growth rate at a low density (calibration of the model)
production = r * estimate * (1 - estimate / k)
correction = estimate * growth / 2 + (estimate - target) / 4
quota = int(round(max(0.0, production + correction)))

bio7.r(f"deerQuota <- {quota}; deerAction <- 'sustainable hunting'")
print(f"  Management: sustainable hunting, quota {quota}")
