"""Daily job: download the latest Copernicus Marine chlorophyll-a, SST and water clarity for Malaysian
waters, compress them to daily_marine_fronts.json (< 200 KB) for the app.

Credentials come from COPERNICUSMARINE_SERVICE_USERNAME / COPERNICUSMARINE_SERVICE_PASSWORD
(https://toolbox-docs.marine.copernicus.eu/en/stable/usage/login-usage.html).
Dataset choices are documented in docs/sumber-data.md.
"""
from __future__ import annotations

import argparse
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

import copernicusmarine
import numpy as np

from pipeline.marine_fronts import BBOX, Grid, bin_to_grid, build_document, encode, front_strength

SST = ("METOFFICE-GLO-SST-L4-NRT-OBS-SST-V2", "analysed_sst")
CHL = ("cmems_obs-oc_glo_bgc-plankton_nrt_l4-gapfree-multi-4km_P1D", "CHL")
ZSD = ("cmems_obs-oc_glo_bgc-transp_nrt_l4-gapfree-multi-4km_P1D", "ZSD")

# The NRT products lag by one to two days; a week back always contains the latest day.
LOOKBACK_DAYS = 7


def latest_day(dataset_id: str, variable: str) -> tuple[np.ndarray, np.ndarray, np.ndarray, date]:
    """Values, latitudes, longitudes and date of the most recent day with data in the bbox."""
    now = datetime.now(timezone.utc)
    ds = copernicusmarine.open_dataset(
        dataset_id=dataset_id,
        variables=[variable],
        minimum_latitude=BBOX["lat_min"],
        maximum_latitude=BBOX["lat_max"],
        minimum_longitude=BBOX["lon_min"],
        maximum_longitude=BBOX["lon_max"],
        start_datetime=(now - timedelta(days=LOOKBACK_DAYS)).strftime("%Y-%m-%dT00:00:00"),
        end_datetime=now.strftime("%Y-%m-%dT23:59:59"),
    )
    field = ds[variable]
    if "depth" in field.dims:
        field = field.isel(depth=0)
    # Newest first, skipping a day that is still entirely empty.
    for index in range(field.sizes["time"] - 1, -1, -1):
        day = field.isel(time=index).load()
        values = day.values.astype(float)
        if np.isfinite(values).any():
            when = np.datetime64(day["time"].values, "D").astype(object)
            return values, ds["latitude"].values, ds["longitude"].values, when
    raise RuntimeError(f"No data for {dataset_id} in the last {LOOKBACK_DAYS} days")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, default=Path("daily_marine_fronts.json"))
    args = parser.parse_args()

    grid = Grid.for_bbox()

    sst_k, sst_lats, sst_lons, sst_day = latest_day(*SST)
    sst_c = sst_k - 273.15
    chl, chl_lats, chl_lons, chl_day = latest_day(*CHL)
    zsd, zsd_lats, zsd_lons, zsd_day = latest_day(*ZSD)

    document = build_document(
        {
            "sst_c": (bin_to_grid(sst_c, sst_lats, sst_lons, grid), sst_day, SST[0], 2),
            # Strongest front inside each cell, so narrow fronts survive the averaging to 0.25 deg.
            "sst_front_c_per_10km": (
                bin_to_grid(front_strength(sst_c, sst_lats, sst_lons), sst_lats, sst_lons, grid, how="max"),
                sst_day, SST[0], 2,
            ),
            "chl_mg_m3": (bin_to_grid(chl, chl_lats, chl_lons, grid), chl_day, CHL[0], 3),
            "zsd_m": (bin_to_grid(zsd, zsd_lats, zsd_lons, grid), zsd_day, ZSD[0], 1),
        },
        grid,
    )
    data = encode(document)
    args.out.write_bytes(data)
    print(f"Wrote {args.out} ({len(data)} bytes): SST {sst_day}, CHL {chl_day}, ZSD {zsd_day}")


if __name__ == "__main__":
    main()
