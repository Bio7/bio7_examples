#This example renders cubes dynamically in Blender. To start Blender from Bio7
#please adjust the Bio7 preferences!
#Example adapted from the Blender website: http://www.blender.org

mylayers = [False]*20
mylayers[0] = True
add_cube = bpy.ops.mesh.primitive_cube_add
for index in range(0, 1):
  for index2 in range(0, 2):
    for index3 in range(0, 10):
      add_cube(location=(index*3, index2*3,  index3*3), layers=mylayers)


