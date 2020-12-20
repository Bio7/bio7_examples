# A Jython example which overrides the ecomain method
# of the inherited Model class of Bio7!
# The method 'run' can be called from the Start/Pause action
# of the Bio7 toolbar!

class jythonClass(Model):

	def run(self):
		#print index
		Field.chance();
		Field.doPaint();

#We instantiate the class!
model = jythonClass()
#We set the class as the compiled class!
Compiled.setModel(model);


