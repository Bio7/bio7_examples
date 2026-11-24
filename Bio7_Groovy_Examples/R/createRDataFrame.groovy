/*
Create a data frame and a matrix in R from Groovy (RUtil, the API of the embedded R).
The columns of a data frame are arrays of the same length (double[], int[], String[] or boolean[]).
*/
import com.eco.bio7.rbridge.RUtil

if (!RUtil.isAlive()) {
	Bio7Dialog.message("R is not running! Please start R first.")
	return
}
/* A data frame (the columns in the order of the map) */
RUtil.dataFrameToR("myDataFrame", [
	col1: ["a1", "a2", "a3", "a4"] as String[],
	col2: ["b1", "b2", "b3", "b4"] as String[],
	col3: [1, 2, 3, 4] as int[],
	col4: [true, false, true, true] as boolean[]
])
println(RUtil.print("myDataFrame"))

/* A matrix: the rows of a Java array */
RUtil.toR("myMatrix", [[1, 2, 3], [4, 5, 6], [7, 8, 9]] as double[][])
println(RUtil.print("myMatrix"))

/* And back: a matrix as rows, a column of the data frame */
double[][] m = RUtil.fromRMatrix("myMatrix %*% myMatrix")
println(m.collect { it.toList() })
println(RUtil.fromRStrings("myDataFrame\$col1").toList())
