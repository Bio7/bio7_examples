import numpy as np, tifffile, json, sys
from skimage import measure
W, H, D = 160, 160, 120
C = np.array([80.0, 80.0, 60.0])
R = 55.0
CAVE = (80.0, 80.0, 58.0, 34.0, 30.0, 18.0)
STALACTITES = [(-18, -8, 11), (-8, 10, 13), (4, -12, 10), (14, 6, 12), (22, -4, 9), (-2, 0, 14), (-20, 12, 9)]   # dx, dy, length
STALAGMITES = [(-12, -14, 7), (10, 14, 8), (20, -14, 6), (-26, 0, 6)]
STONE = (86.0, 76.0, 48.0, 4.0)
RB = 3.5
def ellipsoid(x, y, z, cx, cy, cz, a, b, c):
    X, Y, Z = x - cx, y - cy, z - cz
    q = np.sqrt((X / a) ** 2 + (Y / b) ** 2 + (Z / c) ** 2)
    g = np.sqrt((X / a ** 2) ** 2 + (Y / b ** 2) ** 2 + (Z / c ** 2) ** 2) / np.maximum(q, 1e-9)
    return (1 - q) / np.maximum(g, 1e-9)
def ceiling(dx, dy):
    cx, cy, cz, a, b, c = CAVE
    return cz + c * np.sqrt(max(0.0, 1 - (dx / a) ** 2 - (dy / b) ** 2))
def floor(dx, dy):
    cx, cy, cz, a, b, c = CAVE
    return cz - c * np.sqrt(max(0.0, 1 - (dx / a) ** 2 - (dy / b) ** 2))
def cone(x, y, z, cx, cy, z_base, z_tip, rb):
    """a cone with the base (radius rb) at z_base and the tip at z_tip (positive inside)"""
    h = abs(z_base - z_tip)
    t = (z - z_tip) / (z_base - z_tip)            # 0 at the tip, 1 at the base
    r = rb * np.clip(t, 0, None)
    d = np.sqrt((x - cx) ** 2 + (y - cy) ** 2)
    cos = h / np.sqrt(h * h + rb * rb)
    side = (r - d) * cos
    tip = t * h                                    # below the tip: outside
    return np.minimum(side, tip)
def field(x, y, z):
    outer = R - np.sqrt((x - C[0]) ** 2 + (y - C[1]) ** 2 + (z - C[2]) ** 2)
    cave = ellipsoid(x, y, z, *CAVE)               # positive inside of the cave
    rock_in_cave = -1e9 * np.ones_like(x)
    for dx, dy, L in STALACTITES:                  # hanging from the ceiling, the base inside of the rock
        top = ceiling(dx, dy)
        rock_in_cave = np.maximum(rock_in_cave, cone(x, y, z, CAVE[0] + dx, CAVE[1] + dy, top + 6, top - L, RB * (L + 6) / L))
    for dx, dy, L in STALAGMITES:                  # standing on the floor
        bottom = floor(dx, dy)
        rock_in_cave = np.maximum(rock_in_cave, cone(x, y, z, CAVE[0] + dx, CAVE[1] + dy, bottom - 6, bottom + L, RB * (L + 6) / L))
    cave = np.minimum(cave, -rock_in_cave)         # the stalactites and stalagmites are rock
    material = np.minimum(outer, -cave)
    stone = STONE[3] - np.sqrt((x - STONE[0]) ** 2 + (y - STONE[1]) ** 2 + (z - STONE[2]) ** 2)
    return np.maximum(material, stone), cave, stone
if __name__ == "__main__":
    out = sys.argv[1]
    z, y, x = np.mgrid[0:D, 0:H, 0:W].astype(np.float64) + 0.5
    s, _, _ = field(x, y, z)
    img = np.rint(np.clip(0.5 + s, 0, 1) * 255).astype(np.uint8)
    tifffile.imwrite(out, img, imagej=True, resolution=(1.0, 1.0), metadata={"spacing": 1.0, "unit": "micron", "axes": "ZYX"})
    # the reference values on a 4x finer grid (voxels of 0.25 µm)
    f = 4
    ref = {}
    zz, yy, xx = np.mgrid[0:D * f, 0:H * f, 0:W * f].astype(np.float32)
    xx = (xx + 0.5) / f; yy = (yy + 0.5) / f; zz = (zz + 0.5) / f
    s4, cave4, stone4 = field(xx, yy, zz)
    v = 1.0 / f ** 3
    rock = (s4 > 0) & (stone4 <= 0)
    ref["material"] = float(rock.sum() * v)
    ref["stone"] = float((stone4 > 0).sum() * v)
    outer4 = R - np.sqrt((xx - C[0]) ** 2 + (yy - C[1]) ** 2 + (zz - C[2]) ** 2)
    ref["filled"] = float((outer4 > 0).sum() * v)
    ref["cave_whole"] = float((cave4 > 0).sum() * v)          # the space inside of the cave wall (the stone included)
    ref["cave_free"] = ref["cave_whole"] - ref["stone"]
    ref["outer_area"] = float(4 * np.pi * R * R)
    # the area of the cave wall: marching cubes of the fine cave field (smooth, 0.25 µm)
    verts, faces, _, _ = measure.marching_cubes(np.pad(cave4, 1, constant_values=-1), 0, spacing=(1 / f,) * 3)
    ref["cave_wall_area"] = float(measure.mesh_surface_area(verts, faces))
    smooth = 4 / 3 * np.pi * CAVE[3] * CAVE[4] * CAVE[5]
    ref["cave_without_stalactites"] = float(smooth)
    print(json.dumps(ref, indent=1))
    json.dump(ref, open(out + ".json", "w"), indent=1)
