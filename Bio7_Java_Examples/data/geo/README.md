# Data of the geospatial simulation examples

| File | Content | Source |
|---|---|---|
| `harz_dem.tif` | Elevation (m) of the Harz mountains around the Brocken (Germany), 512 x 512 cells, EPSG:3857 | Terrain Tiles, tile 10/542/339 |
| `harz_landcover.tif` | Land cover classes of the same area (10 tree cover, 20 shrubland, 30 grassland, 40 cropland, 50 built-up, 60 bare, 80 water, 90 herbaceous wetland), 512 x 512 cells, EPSG:3857 (resampled with the mode from 10 m) | ESA WorldCover 2021 (v200) |
| `etna_dem.tif` | Elevation (m) of Mount Etna (Sicily, Italy), 512 x 512 cells, EPSG:3857 | Terrain Tiles, tile 11/1109/791 |
| `release_area.geojson` | A polygon for the species dispersal example (WGS84) | Fictitious example area |
| `senne_dem.tif` | Elevation (m) of the Senne between Hövelhof and Oerlinghausen (8.70–8.80° E, 51.83–51.90° N), 343 x 389 cells (about 20 m), EPSG:3857 | Terrain Tiles, zoom 13 |
| `senne_landcover.tif` | Land cover classes (ESA WorldCover, see above) of the same area, 687 x 779 cells (about 10 m), EPSG:3857 | ESA WorldCover 2021 (v200) |
| `senne_satellite.tif` | A satellite image (RGB, about 5 m, brightened) of the same area, EPSG:3857 | Sentinel-2 cloudless 2017 by EOX |
| `senne_relief.tif` | A drawn map (RGB, about 5 m): the hillshade of the elevation tinted with the colors of the land cover, contour lines every 10 m (50 m stronger), EPSG:3857 | Drawn from `senne_dem.tif` and `senne_landcover.tif` |
| `nuernberg_dem.tif` | Elevation (m) of the sand areas southwest of Nuremberg: Hainberg (Oberasbach), the Rednitz valley, Stein and Zirndorf (10.93–11.03° E, 49.40–49.46° N), 362 x 334 cells (about 20 m), EPSG:3857 | Terrain Tiles, zoom 13 |
| `nuernberg_landcover.tif` | Land cover classes of the same area, 724 x 668 cells (about 10 m), EPSG:3857 | ESA WorldCover 2021 (v200) |
| `nuernberg_satellite.tif` | A satellite image (RGB, about 5 m, brightened) of the same area, EPSG:3857 | Sentinel-2 cloudless 2017 by EOX |
| `nuernberg_relief.tif` | A drawn map of the same area (as `senne_relief.tif`), EPSG:3857 | Drawn from `nuernberg_dem.tif` and `nuernberg_landcover.tif` |

**Terrain Tiles** (Registry of Open Data on AWS, `s3://elevation-tiles-prod`). The tiles combine several elevation sources, among them SRTM (NASA), EU-DEM (produced using Copernicus data and information funded by the European Union) and ETOPO1 (NOAA). See the attribution of the dataset: <https://github.com/tilezen/joerd/blob/master/docs/attribution.md>.

**ESA WorldCover** 2021 (v200): © ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium, licensed under CC BY 4.0 (<https://esa-worldcover.org>). Citation: Zanaga, D., Van De Kerchove, R., Daems, D., et al. 2022. ESA WorldCover 10 m 2021 v200. <https://doi.org/10.5281/zenodo.7254221> Read from the public bucket on AWS (`s3://esa-worldcover`) and resampled to the grid of `harz_dem.tif`.

**Sentinel-2 cloudless 2017** by EOX IT Services GmbH (<https://s2maps.eu>), licensed under CC BY 4.0: "Sentinel-2 cloudless - https://s2maps.eu by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2017)". Read from the WMTS of EOX (layer `s2cloudless-2017_3857`) and brightened (a gamma of 0.8, a gain of 1.15). Note: the later editions of the mosaic (2018 and newer) have a non-commercial license.

The satellite images and the drawn maps are the backgrounds of the examples `SenneGrassland` and `NurembergGrassland` (`background(name, file)`); the land cover gives the habitats, the elevation the moisture (wetter hollows; at Nuremberg also more light on the ridges).

The examples read the files with GDAL (Bio7 → Preferences → GDAL) in `load()`; the HTML export embeds the loaded layers.
