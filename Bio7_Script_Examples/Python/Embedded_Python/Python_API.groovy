/*
 * The Java API of the embedded Python (Groovy, BeanShell or Java), like
 * REval for R. Run this script in the Groovy editor; open an image in
 * ImageJ and start R first for the second and the third part.
 */
import static com.eco.bio7.pybridge.PythonUtil.*
import com.eco.bio7.pyffm.PyValue

/* Starts the embedded Python (the Python of the preferences) if necessary */
start()

/* Java -> Python: numpy arrays (a matrix as 2-d array) */
toPython("x", [1.5, 2.5, 3.0, 4.0] as double[])
toPython("m", [[1, 2, 3], [4, 5, 6]] as double[][])
toPython("names", "Forest", "Meadow")

/* Python -> Java */
PyValue.Array mean = fromPython("x.mean()")
println "Mean of x: " + mean.asDouble()
double[] col = fromPythonDoubles("m[:, 1]")
println "Second column: " + col

/* Code like a script: the output in the console, plots in the Python Plot view */
run("""
import matplotlib.pyplot as plt
plt.figure()
plt.plot(x, x ** 2, 'o-')
plt.title('From Groovy')
plt.show()
""")

/* ImageJ <-> Python (needs an open image) */
try {
	imageToPython("img")
	println "Image in Python: " + eval("str(img.shape)").asString()
	imageFromPython("255 - img if img.dtype.name == 'uint8' else img.max() - img", "Inverted (Python)")
} catch (Exception e) {
	println "ImageJ: " + e.message
}

/* R <-> Python (needs R) */
try {
	rToPython("summary(cars)", "carsSummary")
	println "summary(cars) in Python: " + eval("str(carsSummary)").asString()
	pythonToR("m", "mFromPython")
	println "m in R (same element order as in Python): mFromPython[1, 2] = 2"
} catch (Exception e) {
	println "R: " + e.message
}
