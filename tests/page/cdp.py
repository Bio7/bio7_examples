"""Headless Chrome over the DevTools protocol (the tests of the exported pages).

    with Chrome() as c:
        c.open("file:///.../page.html")
        c.wait("document.getElementById('status').textContent.startsWith('Step')")
        value = c.eval("1 + 1")            # also a Promise (awaited)
        c.shot("page.png")

Chrome: the environment variable CHROME or the usual places of Chrome or Chromium.
Requires: the Python package websocket-client.
"""
import base64
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request

import websocket

PLACES = ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
          "/Applications/Chromium.app/Contents/MacOS/Chromium",
          r"C:\Program Files\Google\Chrome\Application\chrome.exe",
          r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"]


def chrome_path():
    if os.environ.get("CHROME"):
        return os.environ["CHROME"]
    for p in PLACES:
        if os.path.exists(p):
            return p
    for n in ("google-chrome", "google-chrome-stable", "chromium", "chromium-browser"):
        p = shutil.which(n)
        if p:
            return p
    sys.exit("Chrome was not found: set the environment variable CHROME")


class Chrome:
    def __init__(self, width=1400, height=900, port=9333):
        self.width, self.height, self.port = width, height, port
        self.logs = []

    def __enter__(self):
        self.profile = tempfile.mkdtemp(prefix="bio7tests")
        self.proc = subprocess.Popen([chrome_path(), "--headless=new", f"--remote-debugging-port={self.port}", f"--user-data-dir={self.profile}",
                                      "--no-first-run", "--hide-scrollbars", f"--window-size={self.width},{self.height}", "about:blank"],
                                     stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        page = None
        for _ in range(100):
            try:
                tabs = json.load(urllib.request.urlopen(f"http://127.0.0.1:{self.port}/json"))
                page = [t for t in tabs if t.get("type") == "page"][0]
                break
            except Exception:
                time.sleep(0.2)
        if page is None:
            self.proc.kill()
            sys.exit("Chrome did not start")
        self.ws = websocket.create_connection(page["webSocketDebuggerUrl"], timeout=600)
        self.n = 0
        self.send("Runtime.enable")
        self.send("Emulation.setDeviceMetricsOverride", width=self.width, height=self.height, deviceScaleFactor=1, mobile=False)
        return self

    def __exit__(self, *a):
        try:
            self.ws.close()
        finally:
            self.proc.kill()
            self.proc.wait()
            shutil.rmtree(self.profile, ignore_errors=True)

    def send(self, method, **params):
        self.n += 1
        self.ws.send(json.dumps({"id": self.n, "method": method, "params": params}))
        while True:
            m = json.loads(self.ws.recv())
            if m.get("method") == "Runtime.consoleAPICalled":
                self.logs.append(" ".join(str(x.get("value", x.get("description", ""))) for x in m["params"]["args"]))
            elif m.get("method") == "Runtime.exceptionThrown":
                self.logs.append("EXCEPTION " + m["params"]["exceptionDetails"].get("exception", {}).get("description", ""))
            if m.get("id") == self.n:
                return m.get("result", {})

    def eval(self, expr):
        """The value of a JavaScript expression (a Promise is awaited); an exception is raised as RuntimeError."""
        r = self.send("Runtime.evaluate", expression=expr, returnByValue=True, awaitPromise=True)
        if "exceptionDetails" in r:
            raise RuntimeError(r["exceptionDetails"].get("exception", {}).get("description", str(r["exceptionDetails"]))[:500])
        return r.get("result", {}).get("value")

    def open(self, url):
        self.logs = []
        self.send("Page.navigate", url=url)

    def wait(self, expr, timeout=180):
        """Waits until the expression is truthy (False after the timeout)."""
        t0 = time.time()
        while time.time() - t0 < timeout:
            try:
                if self.eval(expr):
                    return True
            except Exception:
                pass
            time.sleep(0.3)
        return False

    def shot(self, path):
        r = self.send("Page.captureScreenshot", format="png")
        with open(path, "wb") as f:
            f.write(base64.b64decode(r["data"]))
