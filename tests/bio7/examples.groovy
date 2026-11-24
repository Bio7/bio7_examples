/*
 * A startup script of Bio7 for the tests (bio7/run_bio7.sh copies it into the folder of the
 * startup scripts of a test workspace): runs each simulation example of the job list in Bio7,
 * writes its state (JSON) and exports it as an HTML page. The settings are in the file
 * bio7tests.properties of the workspace (written by run_bio7.sh):
 *
 *   out      the folder of the results (<name>.json, <name>.html, bio7.log, done)
 *   jobs     the job list: a line "lang|file|milliseconds" (lang: java, py or r)
 *   examples the folder of the bio7_examples repository (the data of the examples)
 */
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.swt.widgets.Display
import groovy.json.JsonOutput

def root = ResourcesPlugin.getWorkspace().getRoot()
def cfg = new Properties()
new File(root.getLocation().toFile(), "bio7tests.properties").withInputStream { cfg.load(it) }
def OUT = new File(cfg.getProperty("out")); OUT.mkdirs()
def EX = new File(cfg.getProperty("examples"))
def logFile = new File(OUT, "bio7.log"); logFile.text = ""
def log = { String t -> logFile << (t + "\n") }
def ui = { c -> Display.getDefault().syncExec(c) }
def model = { com.eco.bio7.methods.Compiled.getModel() }
def copyTree
copyTree = { File from, File to -> to.mkdirs(); from.eachFile { f -> if (f.isDirectory()) copyTree(f, new File(to, f.name)) else new File(to, f.name).bytes = f.bytes } }

Thread.start {
  try {
    Thread.sleep(8000)
    /* The test project: a Java project of Bio7 with the data and the Java classes of the examples */
    def proj = root.getProject("Bio7_Tests")
    if (!proj.exists()) {
      new com.eco.bio7.wizards.java.CreateBio7JdtProject("Bio7_Tests", new org.eclipse.core.runtime.NullProgressMonitor())
    }
    def dir = proj.getLocation().toFile()
    copyTree(new File(EX, "Bio7_Java_Examples/data"), new File(dir, "data"))
    copyTree(new File(EX, "Bio7_Java_Examples/src/simulation"), new File(dir, "src/simulation"))
    proj.refreshLocal(IResource.DEPTH_INFINITE, null)
    def jobs = new File(cfg.getProperty("jobs")).readLines().findAll { it.trim() && !it.startsWith("#") }
    log("jobs: " + jobs.size())
    for (job in jobs) {
      def (lang, path, ms) = job.split("\\|")
      def file = new File(path)
      def name = file.name.replaceAll("\\.[^.]+\$", "") + "_" + lang
      def before = model()
      long t0 = System.currentTimeMillis()
      try {
        ui { org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false) }
        def page = null
        ui { page = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() }
        if (lang == "java") {
          def pkg = (file.text =~ /(?m)^package\s+([\w.]+)/)[0][1]
          def f = proj.getFile("src/" + pkg.replace('.', '/') + "/" + file.name)
          f.getLocation().toFile().getParentFile().mkdirs()
          f.getLocation().toFile().text = file.text
          proj.refreshLocal(IResource.DEPTH_INFINITE, null)
          ui { org.eclipse.ui.ide.IDE.openEditor(page, f) }
          new com.eco.bio7.compile.CompileClassAndMultipleClasses().compileClasses(f, f, page).join()
        } else {
          /* The script in the project (the data paths are searched up to the project) */
          def copy = new File(dir, "scripts"); copy.mkdirs()
          def target = new File(copy, file.name); target.text = file.text
          proj.refreshLocal(IResource.DEPTH_INFINITE, null)
          ui { org.eclipse.ui.ide.IDE.openEditor(page, proj.getFile("scripts/" + file.name)) }
          if (lang == "py") {
            com.eco.bio7.pybridge.EmbeddedPython.start()
            com.eco.bio7.pybridge.EmbeddedPython.runFile(target.getPath())
          } else {
            com.eco.bio7.rbridge.backend.Backends.startEmbedded(); Thread.sleep(500)
            com.eco.bio7.rffm.RSession.current().voidEval("source('" + target.getPath().replace('\\', '/') + "')")
          }
          path = target.getPath()
        }
        for (int k = 0; k < 160 && model().is(before); k++) Thread.sleep(250)
        Thread.sleep(4000)
        def m = model()
        if (m.is(before)) { log(name + " FAILED: the model did not start"); continue }
        /* Some steps at the speed of the model */
        ui { com.eco.bio7.time.Time.setPause(false) }; Thread.sleep(ms as int); ui { com.eco.bio7.time.Time.setPause(true) }; Thread.sleep(800)
        /* The state: the cells, the values or the positions of the agents */
        def values
        if (m instanceof com.eco.bio7.simulation.GridModel) values = m.cells.toList()
        else if (m instanceof com.eco.bio7.simulation.ValueGridModel) values = m.values.toList()
        else values = m.agents.collectMany { a -> a.hasProperty("lon") ? [a.lon, a.lat] : [a.x, a.y, a.z] }
        def kind = m instanceof com.eco.bio7.simulation.GeoGridModel ? "GeoGridModel" : m instanceof com.eco.bio7.simulation.GridModel ? "GridModel"
          : m instanceof com.eco.bio7.simulation.ValueGridModel ? "ValueGridModel" : m instanceof com.eco.bio7.simulation.GeoAgentModel ? "GeoAgentModel" : "AgentModel"
        new File(OUT, name + ".json").text = JsonOutput.toJson([name: name, lang: lang, kind: kind, t: m.t, values: values,
          series: m.getSeries().collectEntries { k, v -> [k, v.collect { it[1] }] }])
        /* The HTML export */
        def r
        if (lang == "java") {
          def o = new com.eco.bio7.simulation.SimulationExport.Options(); o.images = m.getImageFiles(); o.models = m.getModelFiles(); o.seed = m.getStartSeed()
          if (m instanceof com.eco.bio7.simulation.GeoGridModel) o.geo = m.exportData()
          r = com.eco.bio7.simulation.SimulationExport.create(file.text, file.name, com.eco.bio7.actions.ExportSimulation.bio7Classpath(), o)
        } else {
          def o = new com.eco.bio7.simulation.SimulationExport.Options(); o.seed = m.getStartSeed()
          r = com.eco.bio7.simulation.ScriptExport.create(m, file.text, file.name, o)
        }
        if (r.ok()) { r.page.export(new File(OUT, name + ".html").getPath()); log(name + " OK: t=" + m.t + " (" + (System.currentTimeMillis() - t0) + " ms)") }
        else log(name + " FAILED: export errors " + r.errors)
      } catch (Throwable t) { def sw = new StringWriter(); t.printStackTrace(new PrintWriter(sw)); log(name + " FAILED: " + sw) }
    }
  } catch (Throwable t) { def sw = new StringWriter(); t.printStackTrace(new PrintWriter(sw)); log("FAILED: " + sw) }
  finally { new File(OUT, "done").text = "1" }
}
