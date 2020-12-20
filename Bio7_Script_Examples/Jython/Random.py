#This example uses the Bio7 API from Jython to
#randomize and paint values in the Quadgrid view
#of Bio7!
from java.lang import Thread

for index in range (0,100):
		#print index
		Field.chance();
		Field.doPaint();
		#Slow down to give the panel the time to repaint else
		#the repaint events are combined!
		Thread.sleep(20)

