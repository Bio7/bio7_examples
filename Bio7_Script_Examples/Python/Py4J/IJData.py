# A Python Py4J example which transfers values
# from the ImageJ Results table and plots them with
# the Matplot lib.
#Please enable the CPython interpreter in the Bio7 preferences.
#For this example Py4J has to be installed in Python
#see: http://py4j.sourceforge.net/
#and the Py4J Server has to be started!
import numpy as np 
import matplotlib.pyplot as plt 
from py4j.java_gateway import JavaGateway 
from py4j.java_collections import JavaArray

gateway = JavaGateway() 
results=gateway.jvm.ij.measure.ResultsTable 
analyzer=gateway.jvm.ij.plugin.filter.Analyzer

#get the results from the Java ImageJ API!
rt=analyzer.getResultsTable() 
res=rt.getColumn(0) 
lastCol=rt.getLastColumn();

values=[[0 for x in range(len(res))] for x in range(lastCol+1)]
for x in range (0,lastCol+1): 
	vals=rt.getColumnAsDoubles(x) 
	if (isinstance(vals,JavaArray)): 	
		print ('')   		   		
		for y in range (0,len(res)): 			
			values[x][y]=vals[y]

#Plot the values!
plt.boxplot(values) 
plt.show()