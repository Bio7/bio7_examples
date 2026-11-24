# A wildfire in the Harz mountains (Germany) with real elevation and land cover data.
#
# m$load reads the data with GDAL (only in Bio7): the elevation and the ESA WorldCover land cover,
# resampled to a grid of 200 columns. The fire spreads by the land cover (forest, grassland and
# fields, settlements), faster uphill and with the wind. The model runs in ImageJ and on the
# WorldWind globe of Bio7; the exported page shows it on an online map (with the loaded data).
# The data is in data/geo of this project (see its README.md for the sources and licenses).

FOREST <- 0L; BURNING <- 1L; BURNT <- 2L; NO_FUEL <- 3L; OPEN_LAND <- 4L; SETTLEMENT <- 5L

m <- geo_grid_model("Wildfire", "A wildfire in the Harz mountains: the fire spreads by the land cover (forest, grassland and fields, settlements), faster uphill and with the wind.")

m$param("forest", 0.35, min = 0, max = 1, step = 0.01, label = "Spread in the forest", group = "Spread by the land cover")
m$param("open_land", 0.15, min = 0, max = 1, step = 0.01, label = "Spread over grassland and fields", group = "Spread by the land cover")
m$param("settlement", 0.04, min = 0, max = 1, step = 0.01, label = "Spread over settlements", group = "Spread by the land cover")
m$param("slope_effect", 4, min = 0, max = 10, step = 0.1, label = "Effect of the slope (uphill faster)", group = "Terrain", type = "double")
m$param("wind", "West", options = c("No wind", "West", "East", "North", "South"), label = "Wind from", group = "Wind")
m$param("wind_strength", 0.6, min = 0, max = 1, step = 0.05, label = "Strength of the wind", group = "Wind")

m$load <- function(m) {
  m$grid("data/geo/harz_dem.tif", 200)                 # the grid: extent, coordinate system, 200 columns
  m$add_layer("elevation", m$raster("data/geo/harz_dem.tif"))
  # ESA WorldCover classes (10 trees, 20 shrubs, 30 grassland, 40 cropland, 50 built-up, 60 bare, 80 water, ...)
  m$add_layer("landcover", m$raster("data/geo/harz_landcover.tif", 1, "near"))
  m$show_on_globe()                                     # also live on the WorldWind globe of Bio7
  m$attribution(paste("Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data, ETOPO1);",
                      "Land cover: © ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium (CC BY 4.0)"))
}

ignite <- function(m, lon, lat) {
  cx <- m$col_at(lon, lat); cy <- m$row_at(lon, lat)
  if (is.na(cx) || is.na(cy)) return(invisible())
  for (dy in -1:1) for (dx in -1:1) {
    x <- cx + dx; y <- cy + dy
    if (x >= 1 && y >= 1 && x <= m$width && y <= m$height && m$get(x, y) %in% c(FOREST, OPEN_LAND, SETTLEMENT)) m$set(x, y, BURNING)
  }
}

m$init <- function(m) {
  m$palette("#2e7d3240", "#ff5a00", "#2b2420", "#00000000", "#f2d16b30", "#7f7f7f80")   # with alpha: the map below
  m$wrap <- FALSE
  m$elevation <- m$layer("elevation")
  lc <- m$layer("landcover")
  fuel <- matrix(NO_FUEL, nrow(lc), ncol(lc))
  fuel[lc %in% c(10, 95)] <- FOREST
  fuel[lc %in% c(20, 30, 40, 90)] <- OPEN_LAND
  fuel[lc == 50] <- SETTLEMENT
  m$fuel <- fuel
  m$cells <- fuel
  ignite(m, 10.668, 51.788)
}

m$step <- function(m) {
  wind <- switch(m$wind, West = c(1, 0), East = c(-1, 0), North = c(0, 1), South = c(0, -1), c(0, 0))
  c <- m$cells; h <- nrow(c); w <- ncol(c)
  p <- ifelse(c == FOREST, m$forest, ifelse(c == OPEN_LAND, m$open_land, ifelse(c == SETTLEMENT, m$settlement, 0)))
  e <- m$elevation
  catch <- matrix(FALSE, h, w)
  for (dy in -1:1) for (dx in -1:1) {
    if (dx == 0 && dy == 0) next
    rows <- seq_len(h) + dy; cols <- seq_len(w) + dx
    inside <- outer(rows >= 1 & rows <= h, cols >= 1 & cols <= w, "&")
    rc <- pmin(pmax(rows, 1), h); cc <- pmin(pmax(cols, 1), w)
    # the burning neighbors in the direction (dx, dy) and the rise from them to the cell
    near <- c[rc, cc] == BURNING & inside
    diagonal <- if (dx != 0 && dy != 0) 1.4142 else 1
    rise <- (e - e[rc, cc]) / (diagonal * m$cell_size_meters)
    slope <- pmin(pmax(1 + m$slope_effect * rise, 0.2), 3)
    wf <- 1 + m$wind_strength * (-dx * wind[1] - dy * wind[2]) / diagonal
    catch <- catch | (near & m$random(h * w) < p * slope * wf)
  }
  new <- c
  new[c == BURNING] <- BURNT
  new[catch & p > 0] <- BURNING
  m$cells <- new
  ha <- m$cell_size_meters^2 / 10000
  m$chart("Burning area (ha)", m$count(BURNING) * ha)
  m$chart("Burnt area (ha)", m$count(BURNT) * ha)
  m$chart("Burnt settlements (ha)", sum(new == BURNT & m$fuel == SETTLEMENT) * ha)
}

m$action("ignite_brocken", function(m) ignite(m, 10.630, 51.795), label = "Ignite near the Brocken")
m$action("lightning", function(m) ignite(m, m$lon(m$random_int(1, m$width)), m$lat(m$random_int(1, m$height))), label = "Lightning at a random place")

simulation(m)
