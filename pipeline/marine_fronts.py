"""Turn Copernicus Marine grids into the compact daily JSON the app downloads.

Pure numpy so it can be tested without credentials; process_satellite.py does the downloading.
"""
from __future__ import annotations

import json
import math
from dataclasses import dataclass
from datetime import date, datetime, timezone

import numpy as np

# Malaysian waters: Strait of Malacca, South China Sea, Sabah and Sarawak.
BBOX = {"lat_min": 0.5, "lat_max": 7.5, "lon_min": 99.0, "lon_max": 119.5}
GRID_STEP = 0.25
KM_PER_DEGREE = 111.195
FRONT_DISTANCE_KM = 10.0
MAX_BYTES = 200_000


@dataclass(frozen=True)
class Grid:
    lat0: float
    lon0: float
    step: float
    nlat: int
    nlon: int

    @staticmethod
    def for_bbox(bbox: dict = BBOX, step: float = GRID_STEP) -> "Grid":
        nlat = round((bbox["lat_max"] - bbox["lat_min"]) / step)
        nlon = round((bbox["lon_max"] - bbox["lon_min"]) / step)
        return Grid(bbox["lat_min"], bbox["lon_min"], step, nlat, nlon)


def bin_to_grid(values: np.ndarray, lats: np.ndarray, lons: np.ndarray, grid: Grid, how: str = "mean") -> np.ndarray:
    """Averages (or takes the max of) a native lat/lon grid into coarse cells; NaN where a cell has no data."""
    lat2d, lon2d = np.meshgrid(lats, lons, indexing="ij")
    row = np.floor((lat2d - grid.lat0) / grid.step).astype(int)
    col = np.floor((lon2d - grid.lon0) / grid.step).astype(int)
    inside = (row >= 0) & (row < grid.nlat) & (col >= 0) & (col < grid.nlon) & np.isfinite(values)
    cell = row[inside] * grid.nlon + col[inside]
    data = values[inside]
    out = np.full(grid.nlat * grid.nlon, np.nan)
    if how == "mean":
        sums = np.bincount(cell, weights=data, minlength=out.size)
        counts = np.bincount(cell, minlength=out.size)
        np.divide(sums, counts, out=out, where=counts > 0)
    elif how == "max":
        np.fmax.at(out, cell, data)
    else:
        raise ValueError(how)
    return out.reshape(grid.nlat, grid.nlon)


def front_strength(sst_c: np.ndarray, lats: np.ndarray, lons: np.ndarray) -> np.ndarray:
    """Temperature change across 10 km (degC per 10 km) on the native grid, from the local gradient."""
    dlat_km = np.abs(np.gradient(lats)) * KM_PER_DEGREE
    dlon_km = np.abs(np.gradient(lons))[None, :] * KM_PER_DEGREE * np.cos(np.radians(lats))[:, None]
    d_dlat = np.gradient(sst_c, axis=0) / dlat_km[:, None]
    d_dlon = np.gradient(sst_c, axis=1) / dlon_km
    return np.hypot(d_dlat, d_dlon) * FRONT_DISTANCE_KM


def _values(array: np.ndarray, decimals: int) -> list:
    return [None if not math.isfinite(v) else round(float(v), decimals) for v in array.ravel()]


def build_document(layers: dict[str, tuple[np.ndarray, date, str, int]], grid: Grid, generated: datetime | None = None) -> dict:
    """layers: name -> (coarse 2D array, data date, source dataset id, decimals)."""
    return {
        "version": 1,
        "generated_utc": (generated or datetime.now(timezone.utc)).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "grid": {"lat0": grid.lat0, "lon0": grid.lon0, "step": grid.step, "nlat": grid.nlat, "nlon": grid.nlon,
                 "order": "row-major, south to north, west to east; null = no data (land or cloud)"},
        "layers": {
            name: {"date": day.isoformat(), "dataset": dataset, "values": _values(values, decimals)}
            for name, (values, day, dataset, decimals) in layers.items()
        },
    }


def encode(document: dict) -> bytes:
    data = json.dumps(document, separators=(",", ":"), allow_nan=False).encode()
    if len(data) > MAX_BYTES:
        raise ValueError(f"daily_marine_fronts.json is {len(data)} bytes, over the {MAX_BYTES} byte budget")
    return data
