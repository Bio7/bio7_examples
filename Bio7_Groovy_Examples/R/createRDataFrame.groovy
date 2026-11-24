/*A Groovy example to create and transfer a dataframe and matrix!*/

import static com.eco.bio7.rbridge.REval.*;
import com.eco.bio7.rffm.RValue;

/*Create and transfer a dataframe (columns with different types)!*/
def columns = new LinkedHashMap<String, RValue>();
columns.put("col1", RValue.of("a1", "a2", "a3", "a4"));
columns.put("col2", RValue.of("b1", "b2", "b3", "b4"));
columns.put("col3", RValue.of(1, 2, 3, 4));
RValue mydf = RValue.dataFrame(columns);

toR("myDataFrame", mydf);
evalR("print(myDataFrame)", null);

/*Create and transfer a matrix (the rows of a Java array)!*/
def matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]] as double[][]
RValue mat = RValue.Num.matrix(matrix);

toR("myMatrix", mat);
evalR("print(myMatrix)", null);
