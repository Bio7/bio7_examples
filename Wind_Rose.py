# A 3D plot of Bio7 (Plot3D) for Blender: imports the glTF file and adds the texts,
# a camera and the lights in a new scene.
# Run it in Blender: Scripting workspace - Open - Run Script (or blender --python <script>).
import os
import math
import bpy

GLB = "/Users/marcel/git/bio7_examples/Wind_Rose.glb"
NAME = "Wind Rose"
# The animation (a recording): the frames of the objects (first, last), the number of the frames
ANIMATION = {
}
FRAMES = 0
FPS = 24
# The texts: x, y, z, text, size, bold, kind (0 title, 1 number, 2 axis title), first and last frame (0 = always)
TEXT_COLORS = [(0.04231141, 0.04231141, 0.04231141, 1), (0.04231141, 0.04231141, 0.04231141, 1), (0.04231141, 0.04231141, 0.04231141, 1)]
TEXTS = [
    (5.15, 5.95, 0.05, "6.3", 0.3, True, 1, 0, 0),
    (5.15, 6.9, 0.05, "12.5", 0.3, True, 1, 0, 0),
    (5.15, 7.85, 0.05, "18.8", 0.3, True, 1, 0, 0),
    (5.15, 8.8, 0.05, "25.0", 0.3, True, 1, 0, 0),
    (5.0, 9.332, 0.05, "N", 0.405, True, 1, 0, 0),
    (6.6577845, 9.002246, 0.05, "NNE", 0.405, True, 1, 0, 0),
    (8.063187, 8.063187, 0.05, "NE", 0.405, True, 1, 0, 0),
    (9.002246, 6.6577845, 0.05, "ENE", 0.405, True, 1, 0, 0),
    (9.332, 5.0, 0.05, "E", 0.405, True, 1, 0, 0),
    (9.002246, 3.3422153, 0.05, "ESE", 0.405, True, 1, 0, 0),
    (8.063187, 1.9368135, 0.05, "SE", 0.405, True, 1, 0, 0),
    (6.6577845, 0.99775386, 0.05, "SSE", 0.405, True, 1, 0, 0),
    (5.0, 0.668, 0.05, "S", 0.405, True, 1, 0, 0),
    (3.3422153, 0.99775386, 0.05, "SSW", 0.405, True, 1, 0, 0),
    (1.9368135, 1.9368135, 0.05, "SW", 0.405, True, 1, 0, 0),
    (0.99775386, 3.3422153, 0.05, "WSW", 0.405, True, 1, 0, 0),
    (0.668, 5.0, 0.05, "W", 0.405, True, 1, 0, 0),
    (0.99775386, 6.6577845, 0.05, "WNW", 0.405, True, 1, 0, 0),
    (1.9368135, 8.063187, 0.05, "NW", 0.405, True, 1, 0, 0),
    (3.3422153, 9.002246, 0.05, "NNW", 0.405, True, 1, 0, 0),
    (5.0, 5.0, 6.2, "Wind Rose", 0.6, True, 0, 0, 0),
    (5.0, 5.0, 5.45, "192 hours", 0.42, False, 0, 0, 0),
]
EYE = (23.058365, -20.252504, 15.363855)
CENTER = (5.0, 5.0, 1.5)


def find_glb():
    if os.path.exists(GLB):
        return GLB
    # Next to the script (e.g., if the files were moved)
    folders = []
    try:
        folders.append(os.path.dirname(os.path.abspath(__file__)))
    except NameError:
        pass
    for text in bpy.data.texts:
        if text.filepath:
            folders.append(os.path.dirname(bpy.path.abspath(text.filepath)))
    for folder in folders:
        path = os.path.join(folder, "Wind_Rose.glb")
        if os.path.exists(path):
            return path
    raise FileNotFoundError("The glTF file was not found: " + GLB)


# A new scene (the existing scenes are kept)
if bpy.context.window is not None:
    scene = bpy.data.scenes.new(NAME)
    bpy.context.window.scene = scene
else:
    # Background mode (blender --background --python): the current scene
    scene = bpy.context.scene
bpy.ops.import_scene.gltf(filepath=find_glb())
imported = list(bpy.context.selected_objects)


def visible(obj, first, last):
    # The object is visible from the first to the last frame (keyframes of the visibility)
    for attribute in ('hide_viewport', 'hide_render'):
        if first > 1:
            setattr(obj, attribute, True)
            obj.keyframe_insert(attribute, frame=first - 1)
        setattr(obj, attribute, False)
        obj.keyframe_insert(attribute, frame=first)
        if last < FRAMES:
            setattr(obj, attribute, True)
            obj.keyframe_insert(attribute, frame=last + 1)


if FRAMES > 0:
    scene.frame_start = 1
    scene.frame_end = FRAMES
    scene.render.fps = FPS
    for obj in imported:
        # The name of the glTF node (Blender adds .001 to a name which exists)
        name = obj.name
        if len(name) > 4 and name[-4] == '.' and name[-3:].isdigit():
            name = name[:-4]
        frames = ANIMATION.get(name)
        if frames is not None:
            visible(obj, frames[0], frames[1])

# The vertex colors are the colors of the materials
for obj in imported:
    if obj.type != 'MESH' or not obj.data.color_attributes:
        continue
    attribute = obj.data.color_attributes[0].name
    for slot in obj.material_slots:
        material = slot.material
        if material is None or not material.use_nodes:
            continue
        nodes = material.node_tree.nodes
        bsdf = next((n for n in nodes if n.type == 'BSDF_PRINCIPLED'), None)
        if bsdf is None:
            continue
        color = nodes.new('ShaderNodeVertexColor')
        color.layer_name = attribute
        color.location = (bsdf.location.x - 300, bsdf.location.y)
        material.node_tree.links.new(color.outputs['Color'], bsdf.inputs['Base Color'])
        material.node_tree.links.new(color.outputs['Alpha'], bsdf.inputs['Alpha'])

# The camera (looks at the center of the plot)
target = bpy.data.objects.new(NAME + " Center", None)
target.location = CENTER
scene.collection.objects.link(target)
camera_data = bpy.data.cameras.new(NAME + " Camera")
camera_data.angle = math.radians(40)
camera = bpy.data.objects.new(NAME + " Camera", camera_data)
camera.location = EYE
scene.collection.objects.link(camera)
track = camera.constraints.new('TRACK_TO')
track.target = target
track.track_axis = 'TRACK_NEGATIVE_Z'
track.up_axis = 'UP_Y'
scene.camera = camera

# The texts face the camera
text_materials = []
for kind, color in enumerate(TEXT_COLORS):
    material = bpy.data.materials.new(NAME + " Text " + ("Title", "Numbers", "Axis Titles")[kind])
    material.diffuse_color = color
    material.use_nodes = True
    bsdf = material.node_tree.nodes.get('Principled BSDF')
    if bsdf is not None:
        bsdf.inputs['Base Color'].default_value = color
    text_materials.append(material)
texts = bpy.data.collections.new(NAME + " Texts")
scene.collection.children.link(texts)
for x, y, z, body, size, bold, kind, first, last in TEXTS:
    curve = bpy.data.curves.new(body[:20] or "Text", type='FONT')
    curve.body = body
    curve.size = size
    curve.align_x = 'CENTER'
    curve.align_y = 'CENTER'
    curve.extrude = size * 0.02
    if bold:
        curve.offset = size * 0.015
    obj = bpy.data.objects.new("Text " + body[:20], curve)
    obj.location = (x, y, z)
    obj.data.materials.append(text_materials[kind])
    # The texts cast no shadows on the plot
    if hasattr(obj, 'visible_shadow'):
        obj.visible_shadow = False
    constraint = obj.constraints.new('TRACK_TO')
    constraint.target = camera
    constraint.track_axis = 'TRACK_Z'
    constraint.up_axis = 'UP_Y'
    texts.objects.link(obj)
    if FRAMES > 0 and first > 0:
        visible(obj, first, last)

# The lights and a white background
sun_data = bpy.data.lights.new(NAME + " Sun", type='SUN')
sun_data.energy = 3.0
sun = bpy.data.objects.new(NAME + " Sun", sun_data)
sun.rotation_euler = (math.radians(40), math.radians(10), math.radians(-30))
scene.collection.objects.link(sun)
fill_data = bpy.data.lights.new(NAME + " Fill", type='SUN')
fill_data.energy = 1.2
fill = bpy.data.objects.new(NAME + " Fill", fill_data)
fill.rotation_euler = (math.radians(-50), math.radians(-20), math.radians(140))
scene.collection.objects.link(fill)
world = bpy.data.worlds.new(NAME + " World")
world.use_nodes = True
background = world.node_tree.nodes.get('Background')
if background is not None:
    background.inputs['Color'].default_value = (1, 1, 1, 1)
    background.inputs['Strength'].default_value = 1.0
scene.world = world
scene.view_settings.view_transform = 'Standard'
scene.render.resolution_x = 1600
scene.render.resolution_y = 1200
if FRAMES > 0:
    scene.frame_set(1)
print("Bio7 plot imported:", NAME, "frames:", FRAMES)
