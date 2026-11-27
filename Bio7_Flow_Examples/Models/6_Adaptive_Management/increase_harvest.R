# Decision "Too many deer?" = true (in the loop): the quota of the next year removes the
# expected increase and half of the surplus above the target.

a <- deerAssessment
increase <- max(0, a$estimate * (exp(a$growth) - 1))
deerQuota <- round(increase + (a$estimate - deerPar$target) / 2)
deerAction <- "more hunting"
cat("  Management: more hunting, quota", deerQuota, "\n")
