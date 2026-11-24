# Tests of the simulation API

Regression tests of the simulation API of Bio7 (Java, Python, R and the HTML export). They check
that the examples run in Bio7, that the exported pages give the same results as Bio7, and the
functions of the Python and R modules.

```bash
cd tests
./run_tests.sh --bio7 /Applications/Bio7.app          # macOS
./run_tests.sh --bio7 /opt/Bio7                         # Linux: the folder of the program Bio7
./run_tests.sh --bio7 /Applications/Bio7.app --only network_epidemic   # some examples
./run_tests.sh --no-bio7                                # only the offline tests
```

| Part | What it tests |
|---|---|
| `offline/test_python.py`, `offline/test_r.R` | The modules `bio7sim.py` and `bio7sim.R` without Bio7: setup and 50 steps of each example (the protocol of Bio7 and of the page), the same state for the same seed and another for another seed, `stop()`, the input (`click`, `drag`, `key`), `random_at` (the same numbers as Java), the aliases of the API, experiments (repetitions, seeds, designs, errors) |
| `bio7/run_bio7.sh`, `bio7/examples.groovy` | Bio7 runs every simulation example (Java: compiled; Python and R: run) for a few seconds, writes its state (`work/out/<name>.json`) and exports it as a page (`work/out/<name>.html`) |
| `page/compare.py` | Each page is stepped in headless Chrome to the time step of Bio7; the cells, values or positions of the agents must be identical |
| `page/input.py` | The mouse and the keyboard in the pages of HeatDiffusion (click, drag, key) and NetworkEpidemic (a click infects a person), Java, Python and R |
| `page/firefox.py` | The Python and R pages as local files in headless Firefox (the runtime loads, a step runs); skipped without selenium |

## Requirements

- An installed Bio7 (the tests start it with an own workspace, `work/workspace`, and close it at
  the end). The first start with a new workspace may show the welcome page; the tests run anyway.
  Bio7 needs its R and Python for the R and Python examples, and GDAL for the geospatial examples.
- Python 3 with numpy and websocket-client (`pip install numpy websocket-client`; pandas optional;
  selenium for the Firefox test), R (`Rscript`), Google Chrome or Chromium (the variable `CHROME`
  if it is not found), Firefox for the Firefox test.
- The folder of `bio7sim.py` and `bio7sim.R` for the offline tests: by default the repository
  `bio7` beside `bio7_examples`; else `--bio7sim DIR` or the variable `BIO7SIM`.
- The Python and R pages load Pyodide and webR from the internet.

Never use your own workspace for the tests: `bio7/run_bio7.sh` turns on the startup scripts of
Bio7 in the workspace it uses.

## Results

`work/out` (not in the repository): `<name>.json` and `<name>.html` of each example, `bio7.log`
(Bio7), `compare.json`, `input.json`, `firefox.json`, `test_python.json`, `test_r.tsv`. The
exit code of `run_tests.sh` is 0 if all tests passed.
