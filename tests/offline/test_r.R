# Tests the R module of the simulation API (bio7sim.R) without Bio7: the protocol of Bio7 and of the
# page (setup, step, input), determinism, m$stop(), m$random_at, experiments and designs, with the
# R simulation examples.
#
#   Rscript offline/test_r.R [DIR]
#
# DIR: the folder of bio7sim.R (the environment variable BIO7SIM, default: the repository bio7
# beside bio7_examples). Writes work/out/test_r.tsv; the exit status is 1 if a test fails.
# Examples which need Bio7 (images, 3D models, geodata) are skipped.
args <- commandArgs(trailingOnly = FALSE)
script <- normalizePath(sub("^--file=", "", args[grepl("^--file=", args)][1]))
TESTS <- dirname(dirname(script)); REPO <- dirname(TESTS)
dir <- commandArgs(trailingOnly = TRUE)[1]
if (is.na(dir)) dir <- Sys.getenv("BIO7SIM", file.path(dirname(REPO), "bio7", "com.eco.bio7", "src", "com", "eco", "bio7", "simulation"))
if (!file.exists(file.path(dir, "bio7sim.R"))) stop("bio7sim.R was not found in ", dir, " (give the folder or set BIO7SIM)")
SIM <- file.path(REPO, "Bio7_R_Examples", "Simulation")
OUT <- file.path(TESTS, "work", "out"); dir.create(OUT, recursive = TRUE, showWarnings = FALSE)
e <- attach(NULL, name = "bio7ffm"); assign(".bio7_call", function(op, args, value) invisible(NULL), envir = e)
source(file.path(dir, "bio7sim.R"))
res <- list()
check <- function(name, ok, detail = "") res[[length(res) + 1]] <<- list(test = name, ok = ok, detail = detail)
load_model <- function(path) { simulation <<- function(m) assign(".bio7sim_model", m, envir = globalenv()); source(path, local = new.env()); get(".bio7sim_model", envir = globalenv()) }
digest <- function(s) { s$more_charts <- NULL; s$inputs <- NULL; s$chart_names <- NULL; paste(format(sum(nchar(capture.output(dput(s))))), substr(paste(as.hexmode(utf8ToInt(paste(capture.output(dput(s)), collapse = ""))) %% 251, collapse = ""), 1, 0), sprintf("%.10g", sum(unlist(lapply(s, function(v) if (is.numeric(v)) sum(v * seq_along(v)) else 0)))) ) }
run <- function(seed, steps) {
  s <- .bio7sim_do("setup", as.character(seed))
  for (k in seq_len(steps)) { s <- .bio7sim_do("step"); if (isTRUE(s$stopped == 1)) break }
  list(d = digest(s), t = get(".bio7sim_model", envir = globalenv())$t)
}
for (path in sort(Sys.glob(file.path(SIM, "*.R")))) {
  name <- basename(path)
  if (grepl("_experiment|_sensitivity", name)) next
  r <- tryCatch({
    load_model(path); t0 <- Sys.time()
    a <- run(42, 50); b <- run(42, 50); c <- run(7, 50)
    check(paste0(name, ": setup and 50 steps"), TRUE, sprintf("t=%d, %.1f s", a$t, as.numeric(difftime(Sys.time(), t0, units = "secs")) / 3))
    check(paste0(name, ": same seed, same state"), identical(a$d, b$d), a$d)
    check(paste0(name, ": other seed, other state"), !identical(a$d, c$d) || name == "heat_diffusion.R", c$d)
  }, error = function(err) {
    needs <- name == "wildfire.R" || grepl("bio7|file|cannot open", conditionMessage(err), ignore.case = TRUE)
    check(paste0(name, ": setup and 50 steps"), if (needs) NA else FALSE, paste0(if (needs) "needs Bio7 (images, 3D models or geodata): " else "", substr(conditionMessage(err), 1, 160)))
  })
}
m <- load_model(file.path(SIM, "network_epidemic.R"))
invisible(.bio7sim_do("setup", "42")); assign("recovery", 0.3, envir = m); n <- 0
repeat { s <- .bio7sim_do("step"); n <- n + 1; if (s$stopped == 1 || n > 2000) break }
check("stop(): the run ends (network_epidemic, recovery 0.3)", n <= 2000, paste("stopped at t =", m$t))
m <- load_model(file.path(SIM, "heat_diffusion.R"))
s <- .bio7sim_do("setup", "42")
check("inputs reported in the state", identical(sort(s$inputs), c("click", "drag", "key")), paste(s$inputs, collapse = ","))
invisible(.bio7sim_do("input", "click\u001f60.5\u001f60.5")); check("input click (heat_diffusion)", m$values[61, 61] == 1)
invisible(.bio7sim_do("input", "drag\u001f10\u001f100")); check("input drag", m$values[101, 11] == 1)
invisible(.bio7sim_do("input", "key\u001fc")); check("input key c", sum(m$values) == 0)
m <- load_model(file.path(SIM, "network_epidemic.R"))
t <- experiment(m, params = list(infection = c(0.02, 0.05)), steps = 20, repetitions = 2, every = 10)
check("experiment: runs = combinations x repetitions", length(unique(t$run)) == 4, paste(nrow(t), "rows"))
check("experiment: seeds 42, 43 per combination", identical(sort(unique(t$seed)), c(42, 43)))
t2 <- experiment(m, design = data.frame(infection = c(0.02, 0.08), people = c(100.6, 200.2)), steps = 10, every = 10)
check("experiment design: point column, int rounding", identical(t2$point[t2$step == 0], 1:2) && t2$people[1] == 101, paste(t2$point[t2$step == 0], t2$people[t2$step == 0], collapse = " "))
again <- experiment(m, params = list(infection = 0.02), steps = 20, every = 10)
check("experiment: run 1 repeatable", identical(t$`Infected share`[t$run == 1], again$`Infected share`))
er <- tryCatch({ experiment(m, design = data.frame(infektion = 0.1), steps = 1); "no error" }, error = function(e) conditionMessage(e))
check("experiment: unknown parameter -> error", grepl("Not a parameter", er), er)
check("parameters restored after an experiment", m$infection == 0.05, paste("infection", m$infection))
m <- agent_model("T", ""); m$seed(42); m$t <- 3
java <- c(0.8950467265676707, 0.9944779181387275, 0.13833033409900963, 0.7390363635495305)
check("random_at: the same numbers as randomAt of Java", isTRUE(all.equal(c(m$random_at(5), m$random_at(5, 2), m$random_at(6), m$random_at(7)), java, tolerance = 0)))
check("random_at: a vector of indices", identical(m$random_at(c(5, 6, 7)), java[c(1, 3, 4)]))
m$agents <- data.frame(x = c(0, 3, 50), y = c(0, 4, 50), z = c(0, 0, 0))
check("agent_distance (agent model)", m$agent_distance(1, 2) == 5)
m$link(c(1, 2), c(2, 3)); check("link_count", m$link_count() == 2 && nrow(m$links()) == 2)
before <- m$is_stopped(); m$stop(); check("is_stopped", !before && m$is_stopped())
writeLines(vapply(res, function(r) paste(r$test, if (is.na(r$ok)) "SKIP" else if (r$ok) "OK" else "FAIL", r$detail, sep = "\t"), ""), file.path(OUT, "test_r.tsv"))
cat(sum(vapply(res, function(r) isTRUE(r$ok), TRUE)), "of", length(res), "passed,", sum(vapply(res, function(r) is.na(r$ok), TRUE)), "skipped\n")
for (r in res) if (isFALSE(r$ok)) cat("FAILED", r$test, r$detail, "\n")
if (any(vapply(res, function(r) isFALSE(r$ok), TRUE))) quit(status = 1)
