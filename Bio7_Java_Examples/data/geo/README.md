# Data of the geospatial simulation examples

| File | Content | Source |
|---|---|---|
| `harz_dem.tif` | Elevation (m) of the Harz mountains around the Brocken (Germany), 512 x 512 cells, EPSG:3857 | Terrain Tiles, tile 10/542/339 |
| `harz_landcover.tif` | Land cover classes of the same area (10 tree cover, 20 shrubland, 30 grassland, 40 cropland, 50 built-up, 60 bare, 80 water, 90 herbaceous wetland), 512 x 512 cells, EPSG:3857 (resampled with the mode from 10 m) | ESA WorldCover 2021 (v200) |
| `etna_dem.tif` | Elevation (m) of Mount Etna (Sicily, Italy), 512 x 512 cells, EPSG:3857 | Terrain Tiles, tile 11/1109/791 |
| `release_area.geojson` | A polygon for the species dispersal example (WGS84) | Fictitious example area |

**Terrain Tiles** (Registry of Open Data on AWS, `s3://elevation-tiles-prod`). The tiles combine several elevation sources, among them SRTM (NASA), EU-DEM (produced using Copernicus data and information funded by the European Union) and ETOPO1 (NOAA). See the attribution of the dataset: <https://github.com/tilezen/joerd/blob/master/docs/attribution.md>.

**ESA WorldCover** 2021 (v200): © ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium, licensed under CC BY 4.0 (<https://esa-worldcover.org>). Citation: Zanaga, D., Van De Kerchove, R., Daems, D., et al. 2022. ESA WorldCover 10 m 2021 v200. <https://doi.org/10.5281/zenodo.7254221> Read from the public bucket on AWS (`s3://esa-worldcover`) and resampled to the grid of `harz_dem.tif`.

The examples read the files with GDAL (Bio7 → Preferences → GDAL) in `load()`; the HTML export embeds the loaded layers.
