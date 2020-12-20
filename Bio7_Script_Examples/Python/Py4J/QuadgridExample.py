#Example to call Bio7 Quadgrid from CPython
#Please enable the CPython interpreter in the Bio7 preferences.
#Py4J has to be installed in Python
#See: http://py4j.sourceforge.net/
from py4j.java_gateway import JavaGateway
from py4j.java_gateway import java_import
from time import sleep
gateway = JavaGateway() # connect to the JVM

java_import(gateway.jvm,'com.eco.bio7.discrete.*')

for x in range (1,10):
		gateway.jvm.Field.chance()
		gateway.jvm.com.eco.bio7.discrete.Field.doPaint()
		#sleep(0.1)
		
