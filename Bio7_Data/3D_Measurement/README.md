# 3D Measurement Data

Example stacks for the 3D perspective of Bio7 (tabs *Iso Surface* and *Measure*).
The stacks are ImageJ TIFF files in ZIP archives: open them directly in ImageJ
(*File › Open…*), the calibration is kept.

## Cave_Example.zip

A closed rock with a cave inside – for the **Volume tool** (*Object: Material / Filled*,
*Cavity: Whole space / Free space*), the clipping box and the surface eraser.

- 160 × 160 × 120 voxels, 8 bit, 1 µm voxels; values: a linear ramp of one voxel around
  the surfaces (iso value 127.5 = the exact shapes)
- **Rock:** a sphere, radius 55 µm, closed
- **Cave:** an ellipsoid (semi-axes 34 × 30 × 18 µm), enclosed by the rock
- **7 stalactites** and **4 stalagmites** (cones of rock, part of the cave wall)
- **A stone** (radius 4 µm) lying in the cave without touching the rock (an object of its own)

Reference values (a grid 4 × finer than the stack):

| Measurement | Volume [µm³] | Surface area [µm²] |
|---|---|---|
| Object: Material (rock without the cave) | 621 386 | 48 097 (outer surface + cave wall) |
| Object: Filled (inside of the outer surface) | 696 926 | 38 013 (outer surface) |
| Cavity: Whole space (the cave with the stone) | 75 539 | 10 084 (cave wall) |
| Cavity: Free space (the cave without the stone) | 75 270 | |
| The stone | 270 | |

Bio7 (iso value 127.5, mesh smoothing 5, resolution 1x) measures these values within
0.06 % (the small stone: −5 %, with the measurement resolution 2x −2.6 %).
`make_cave_example.py` creates the stack (Python with NumPy, tifffile, scikit-image).

## particles_1000.zip, particles_5000.zip, particles_10000.zip

Many small, separate spheres – for **Measure Iso Surface Objects**, the object sizes, the
measurement resolution and **Measure Distances** (nearest neighbors, matrices).

| Stack | Voxels | Spheres |
|---|---|---|
| particles_1000 | 256 × 256 × 128 | 1 000 |
| particles_5000 | 512 × 512 × 160 | 5 000 |
| particles_10000 | 512 × 512 × 320 | 10 000 |

- 8 bit, binary (0 / 255), voxels of 0.5 × 0.5 × 0.5 µm
- Sphere radii 2 – 5 voxels; the spheres don't touch (each is an object)

`particles_N_truth.csv` lists every sphere: the number, the center (X, Y, Z in µm),
the radius in voxels, the number of voxels, the volume of the voxels and the volume of a
sphere with the radius + 0.5 voxel (µm³) – the reference for the measured objects.
Small spheres are measured more accurately with *Measurement Resolution* 2x.

See also: the Bio7 tutorial *Measuring Objects and Cavities with the Volume Tool*.
