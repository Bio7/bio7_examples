// The JavaScript of the widget example (WindowBuilder JavaScript; the design:
// src/gui/windowbuilder/AllWidgetsGui.java, index.html is created from it). Bio7 writes this
// file only once: this code stays when index.html is created again.
//
// Each widget of the design is an HTML element with the name of its variable as id
// (document.getElementById("scaleValue")). The tab Outputs shows the values of all
// inputs (and their JavaScript type: the value of an input is a string, a check box
// gives checked, a multiple list the selected options).

// The value of an element: check boxes (checked), multiple lists (an array), radio
// buttons (the group: the checked one), tabs (the active tab), else the value
function value(id) {
  const e = document.getElementById(id);
  if (e && e.type === "checkbox") return e.checked;
  if (e && e.tagName === "SELECT" && e.multiple) return Array.from(e.selectedOptions).map(o => o.value);
  if (e && e.classList.contains("wb-tabs")) return e.querySelector(":scope > .wb-tabbar > button.active").textContent;
  if (e && e.value !== undefined) return e.value;
  const radio = document.querySelector('input[name="' + id + '"]:checked');
  return radio ? radio.value : null;
}

// The ids of the inputs of the design (the names of the variables of the GUI class)
const ids = ["scaleValue", "sliderValue", "spinnerCount", "spinnerRate", "txtName", "txtNotes", "txtSecret",
  "dateDay", "timeOfDay", "comboColor", "listSpecies", "listHabitats", "chkActive", "tglMode",
  "grpSize", "chkOne", "chkTwo", "tabFolder", "ctabFolder"];

const btnPress = document.getElementById("btnPress");
const outPressed = document.getElementById("outPressed");
const canvasPlot = document.getElementById("canvasPlot");
const tableValues = document.getElementById("tableValues");
const outText = document.getElementById("outText");
const outValues = document.getElementById("outValues");
const browserInfo = document.getElementById("browserInfo");

let pressed = 0;

function values() {
  const v = {};
  ids.forEach(id => v[id] = value(id));
  return v;
}

// Canvas: drawn with JavaScript - a histogram of normal values (the number of the Scale, the color of the Combo)
function drawCanvasPlot(v) {
  const g = canvasPlot.getContext("2d");
  const w = canvasPlot.width, h = canvasPlot.height;
  const n = 10 + 10 * Number(v.scaleValue);
  const bins = new Array(20).fill(0);
  for (let k = 0; k < n; k++) {
    /* A normal value (Box-Muller), the bins from -4 to 4 */
    const z = Math.sqrt(-2 * Math.log(1 - Math.random())) * Math.cos(2 * Math.PI * Math.random());
    const b = Math.floor((z + 4) / 8 * bins.length);
    if (b >= 0 && b < bins.length) bins[b]++;
  }
  const max = Math.max(...bins);
  g.clearRect(0, 0, w, h);
  g.fillStyle = v.comboColor.toLowerCase();
  bins.forEach((c, i) => {
    const bh = c / max * (h - 30);
    g.fillRect(i * w / bins.length + 1, h - 20 - bh, w / bins.length - 2, bh);
  });
  g.fillStyle = "#888";
  g.font = "12px system-ui, sans-serif";
  g.fillText(n + " normal values (Scale), color of the Combo", 8, 14);
}

function update() {
  const v = values();
  // Label "out...": a span (textContent)
  outText.textContent = "Hello " + v.txtName + ": color " + v.comboColor + ", size " + v.grpSize + (v.chkActive ? ", active" : "") + ".";
  // Text "out...": a pre
  outValues.textContent = JSON.stringify(v, null, 2);
  // Table: a table (rows of the values and their types)
  tableValues.innerHTML = "<tr><th>Element</th><th>Value</th><th>Type</th></tr>"
    + ids.map(id => "<tr><td>" + id + "</td><td>" + (Array.isArray(v[id]) ? v[id].join(", ") || "(none)" : String(v[id]))
      + "</td><td>" + (Array.isArray(v[id]) ? "array" : typeof v[id]) + "</td></tr>").join("");
  // Browser: a div (HTML)
  browserInfo.innerHTML = "<b>HTML of the JavaScript code:</b> <span style=\"color:" + v.comboColor.toLowerCase() + "\">" + v.txtName + "</span>";
  drawCanvasPlot(v);
}

// Button (push): a button (click event)
btnPress.addEventListener("click", () => {
  pressed++;
  outPressed.textContent = "The button was pressed " + pressed + " times.";
});

// Each change of an input (and a click on a tab) updates the outputs
document.addEventListener("input", update);
document.addEventListener("change", update);
document.querySelectorAll(".wb-tabbar button").forEach(b => b.addEventListener("click", () => setTimeout(update)));
update();
