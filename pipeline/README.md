# Satellite pipeline

Daily GitHub Actions job (`.github/workflows/process_copernicus.yml`) that downloads Copernicus Marine
chlorophyll-a, sea surface temperature and water clarity for Malaysian waters, averages them onto a
0.25 degree grid and publishes `daily_marine_fronts.json` (under 200 KB) to the `latest-data` release:

https://github.com/ArmanFarizul/fishing-copilot/releases/download/latest-data/daily_marine_fronts.json

## JSON layout

- `grid`: `lat0`, `lon0` (south-west corner), `step` in degrees, `nlat` x `nlon` cells.
- `layers.<name>.values`: one value per cell, row-major from south to north and west to east;
  `null` where there is no data (land, or no retrieval).
- Layers: `sst_c` (degC), `sst_front_c_per_10km` (strongest temperature change across 10 km inside the
  cell; the spec calls 1.0 or more a front), `chl_mg_m3`, `zsd_m` (Secchi depth, water clarity).
- Each layer carries its own `date`, because the products lag by different amounts.

## Run locally

```
python -m venv .venv && .venv/bin/pip install -r pipeline/requirements.txt
.venv/bin/python -m pytest pipeline/tests -q
COPERNICUSMARINE_SERVICE_USERNAME=... COPERNICUSMARINE_SERVICE_PASSWORD=... \
  .venv/bin/python -m pipeline.process_satellite --out daily_marine_fronts.json
```
