#!/bin/bash
# Runs the simulation examples in Bio7 and exports them as HTML pages (macOS and Linux).
#
#   bio7/run_bio7.sh --bio7 <Bio7 folder> [--workspace DIR] [--out DIR] [--lang java,py,r] [--only NAME] [--play MS]
#
#   --bio7       the installed Bio7 (macOS: /Applications/Bio7.app, Linux: the folder of the program Bio7)
#   --workspace  a workspace only for the tests (default: tests/work/workspace); never your own workspace:
#                the script turns on the startup scripts of Bio7 in it
#   --out        the results (default: tests/work/out): <name>.json (the state), <name>.html (the page), bio7.log
#   --lang       the languages (default: java,py,r)
#   --only       only the examples whose file name contains the text (several: separated by commas)
#   --play       how long each example runs (milliseconds; default: Java 3000, Python 4000, R 5000)
#
# Bio7 is started with the workspace, runs all examples and is closed when they are done.
set -euo pipefail
TESTS="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$TESTS/.." && pwd)"
BIO7=""; WS="$TESTS/work/workspace"; OUT="$TESTS/work/out"; LANGS="java,py,r"; ONLY=""; PLAY=""
while [ $# -gt 0 ]; do
  case "$1" in
    --bio7) BIO7="$2"; shift 2 ;;
    --workspace) WS="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --lang) LANGS="$2"; shift 2 ;;
    --only) ONLY="$2"; shift 2 ;;
    --play) PLAY="$2"; shift 2 ;;
    *) echo "Unknown option $1"; exit 1 ;;
  esac
done
[ -n "$BIO7" ] || { echo "Give the installed Bio7 with --bio7"; exit 1; }
if [ -x "$BIO7/Contents/MacOS/Bio7" ]; then EXE="$BIO7/Contents/MacOS/Bio7"; elif [ -x "$BIO7/Bio7" ]; then EXE="$BIO7/Bio7"; else echo "Bio7 was not found in $BIO7"; exit 1; fi
mkdir -p "$WS" "$OUT" "$TESTS/work/startup"
rm -f "$OUT"/*.json "$OUT"/*.html "$OUT/done" "$OUT/bio7.log"

# The job list: all models of the simulation examples (not the experiments and analyses)
JOBS="$TESTS/work/jobs.txt"; : > "$JOBS"
add() { # lang file ms
  case ",$LANGS," in *",$1,"*) ;; *) return ;; esac
  if [ -n "$ONLY" ]; then
    local hit=0 o
    IFS=',' read -ra PARTS <<< "$ONLY"
    for o in "${PARTS[@]}"; do [[ "$(basename "$2")" == *"$o"* ]] && hit=1; done
    [ $hit = 1 ] || return 0
  fi
  echo "$1|$2|${PLAY:-$3}" >> "$JOBS"
}
for f in "$REPO"/Bio7_Java_Examples/src/simulation/*.java "$REPO"/Bio7_Java_Examples/src/simulation/api/*.java; do
  case "$f" in *Settings.java) ;; *) add java "$f" 3000 ;; esac
done
for f in "$REPO"/Bio7_Python_Examples/Simulation/*.py; do case "$f" in *_experiment*|*_sensitivity*) ;; *) add py "$f" 4000 ;; esac; done
for f in "$REPO"/Bio7_R_Examples/Simulation/*.R; do case "$f" in *_experiment*|*_sensitivity*) ;; *) add r "$f" 5000 ;; esac; done
echo "$(wc -l < "$JOBS" | tr -d ' ') examples"

# The settings of the startup script and the startup scripts of Bio7 in the test workspace
printf 'out=%s\njobs=%s\nexamples=%s\n' "$OUT" "$JOBS" "$REPO" > "$WS/bio7tests.properties"
cp "$TESTS/bio7/examples.groovy" "$TESTS/work/startup/examples.groovy"
PREFS="$WS/.metadata/.plugins/org.eclipse.core.runtime/.settings/com.eco.bio7.prefs"
mkdir -p "$(dirname "$PREFS")"; touch "$PREFS"
grep -v '^FilesDirectory=\|^STARTUP_SCRIPTS=' "$PREFS" > "$PREFS.tmp" || true
printf 'FilesDirectory=%s\nSTARTUP_SCRIPTS=true\n' "$TESTS/work/startup" >> "$PREFS.tmp"; mv "$PREFS.tmp" "$PREFS"

# Bio7 with the test workspace until the startup script is done
"$EXE" -data "$WS" > "$OUT/bio7_console.log" 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null || true' EXIT
for _ in $(seq 1 3600); do [ -f "$OUT/done" ] && break; kill -0 $PID 2>/dev/null || break; sleep 1; done
sleep 1
kill $PID 2>/dev/null || true
OKS=$(grep -c ' OK:' "$OUT/bio7.log" 2>/dev/null || true); FAILS=$(grep -c 'FAILED' "$OUT/bio7.log" 2>/dev/null || true)
echo "Bio7: ${OKS:-0} OK, ${FAILS:-0} failed (see $OUT/bio7.log)"
