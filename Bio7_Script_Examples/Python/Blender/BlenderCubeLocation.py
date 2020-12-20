#This example renders cubes dynamically in Blender and adds the location of a cube
#to the IPO of Blender. 
#To start Blender from Bio7 please adjust the Bio7 preferences!
#Example adapted from the Blender website: http://www.blender.org

bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete()
#bpy.ops.object.delete()
mylayers = [False]*20
mylayers[0] = True
add_cube = bpy.ops.mesh.primitive_cube_add
count=0;
for index in range(0, 5):
  for index2 in range(0, 5):
    for index3 in range(0, 5):
      add_cube(location=(index*3, index2*3,  index3*3), layers=mylayers)
      obj=bpy.data.objects['Cube']
      obj.location[0] = index*3
      obj.location[1] = index2*3
      obj.location[2] = index3*3
      obj.keyframe_insert(data_path="location", frame=count)
      count+=1
      