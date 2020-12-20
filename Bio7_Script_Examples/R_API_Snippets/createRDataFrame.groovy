/*A Groovy example to create and transfer a dataframe and matrix!*/

import static com.eco.bio7.rbridge.RServeUtil.*;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPInteger;

/*Create and transfer a dataframe (column with different types)!*/
String []colNames = ["col1", "col2", "col3"];
String[] col1 = [ "a1", "a2", "a3", "a4" ];
String[] col2 = [ "b1", "b2", "b3", "b4" ];
int [] col3 = [ 1, 2, 3, 4 ];

column1=new REXPString(col1)
column2=new REXPString(col2)
column3=new REXPInteger(col3)
REXP[] exp=[column1, column2, column3]

REXP mydf = REXP.createDataFrame(new RList(exp, colNames));

toR("myDataFrame", mydf);
evalR("print(myDataFrame)",null);

/*Create and transfer a matrix!*/
def matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]] as double[][]
REXP mat = REXP.createDoubleMatrix(matrix);

toR("myMatrix", mat);
evalR("print(myMatrix)",null);