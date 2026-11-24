#!/bin/bash
# Runs the tests of the simulation API (macOS and Linux):
#
#   ./run_tests.sh --bio7 <Bio7 folder> [--bio7sim DIR] [--only NAME] [--lang java,py,r] [--no-bio7] [--no-firefox]
#
#   1. offline: the Python and R modules without Bio7 (offline/test_python.py, offline/test_r.R)
#   2. Bio7: all simulation examples run in Bio7 and exported as pages (bio7/run_bio7.sh)
#   3. the pages: stepped in headless Chrome and compared with Bio7 (page/compare.py),
#      the mouse and the keyboard (page/input.py), Python and R pages in Firefox (page/firefox.py)
#
# The results are in work/out. See README.md for the requirements.
set -uo pipefail
TESTS="$(cd "$(dirname "$0")" && pwd)"
BIO7=""; BIO7SIM_ARG=""; ONLY=""; LANGS="java,py,r"; RUN_BIO7=1; RUN_FIREFOX=1
while [ $# -gt 0 ]; do
  case "$1" in
    --bio7) BIO7="$2"; shift 2 ;;
    --bio7sim) BIO7SIM_ARG="$2"; shift 2 ;;
    --only) ONLY="$2"; shift 2 ;;
    --lang) LANGS="$2"; shift 2 ;;
    --no-bio7) RUN_BIO7=0; shift ;;
    --no-firefox) RUN_FIREFOX=0; shift ;;
    *) echo "Unknown option $1"; exit 1 ;;
  esac
done
OUT="$TESTS/work/out"; mkdir -p "$OUT"
FAILED=0
step() { echo; echo "== $1"; }

step "Offline: the Python module"
python3 "$TESTS/offline/test_python.py" ${BIO7SIM_ARG:+--bio7sim "$BIO7SIM_ARG"} | tail -3 || FAILED=1
step "Offline: the R module"
Rscript "$TESTS/offline/test_r.R" ${BIO7SIM_ARG:+"$BIO7SIM_ARG"} | tail -3 || FAILED=1

if [ $RUN_BIO7 = 1 ]; then
  [ -n "$BIO7" ] || { echo "Give the installed Bio7 with --bio7 (or --no-bio7)"; exit 1; }
  step "Bio7: the examples (run and exported)"
  "$TESTS/bio7/run_bio7.sh" --bio7 "$BIO7" --lang "$LANGS" ${ONLY:+--only "$ONLY"} || FAILED=1
  step "The pages compared with Bio7 (Chrome)"
  python3 "$TESTS/page/compare.py" "$OUT" || FAILED=1
  step "The mouse and the keyboard in the pages (Chrome)"
  python3 "$TESTS/page/input.py" "$OUT" || FAILED=1
  if [ $RUN_FIREFOX = 1 ]; then
    step "The Python and R pages in Firefox"
    python3 "$TESTS/page/firefox.py" "$OUT"; r=$?; [ $r = 1 ] && FAILED=1
  fi
fi
echo
[ $FAILED = 0 ] && echo "All tests passed" || echo "Some tests failed (see the output above and $OUT)"
exit $FAILED
