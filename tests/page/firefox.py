"""Opens the exported Python and R pages as local files in headless Firefox: the runtime (Pyodide,
webR) loads and a step runs (Firefox gives a local file no origin, see the loading of webR).

    python3 page/firefox.py [OUT]        (default: tests/work/out)

Requires Firefox and the Python package selenium (it finds the driver of Firefox itself).
Writes OUT/firefox.json; the exit code is 1 if a page fails, 2 if selenium is missing.
"""
import glob
import json
import os
import sys
import time

OUT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "work", "out"))

try:
    from selenium import webdriver
    from selenium.webdriver.firefox.options import Options
except ImportError:
    print("Firefox test skipped: the Python package selenium is not installed (pip install selenium)")
    sys.exit(2)


def main():
    o = Options()
    o.add_argument("-headless")
    d = webdriver.Firefox(options=o)
    results = []
    try:
        pages = sorted(glob.glob(os.path.join(OUT, "*_py.html")) + glob.glob(os.path.join(OUT, "*_r.html")))
        for page in pages:
            name = os.path.basename(page)[:-5]
            d.get("file://" + page.replace(os.sep, "/"))
            status = lambda: d.execute_script("return document.getElementById('status').textContent")
            error = lambda: d.execute_script("const e = document.getElementById('error'); return e ? e.textContent : ''")
            t0 = time.time()
            while time.time() - t0 < 180 and not status().startswith("Step") and not error():
                time.sleep(0.5)
            ok, detail = False, ""
            if status().startswith("Step") and not error():
                d.execute_script("document.getElementById('step').click()")
                t1 = time.time()
                while time.time() - t1 < 60 and status() != "Step 1" and not error():
                    time.sleep(0.3)
                ok = status() == "Step 1" and not error()
            detail = error() or status()
            results.append({"page": name, "ok": ok, "detail": detail})
            print("%-24s %s %s" % (name, "pass" if ok else "FAIL", "" if ok else detail), flush=True)
    finally:
        d.quit()
    json.dump(results, open(os.path.join(OUT, "firefox.json"), "w"), indent=1)
    passed = sum(r["ok"] for r in results)
    print("%d of %d pages run in Firefox" % (passed, len(results)))
    sys.exit(0 if passed == len(results) else 1)


if __name__ == "__main__":
    main()
