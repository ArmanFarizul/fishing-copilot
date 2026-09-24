import json
import math
from datetime import date, datetime, timezone

import numpy as np
import pytest

from pipeline.marine_fronts import BBOX, Grid, bin_to_grid, build_document, encode, front_strength


def native_axes(step):
    lats = np.arange(BBOX["lat_min"] + step / 2, BBOX["lat_max"], step)
    lons = np.arange(BBOX["lon_min"] + step / 2, BBOX["lon_max"], step)
    return lats, lons


def test_grid_covers_the_bbox_at_quarter_degree():
    grid = Grid.for_bbox()
    assert (grid.nlat, grid.nlon) == (28, 82)


def test_mean_binning_averages_native_points_and_skips_nan():
    grid = Grid.for_bbox()
    lats, lons = native_axes(0.05)
    values = np.full((lats.size, lons.size), 2.0)
    values[:5, :5] = 4.0          # the first coarse cell holds 25 native points; all are 4 here
    values[5, 0] = np.nan         # a cloud gap in another cell
    coarse = bin_to_grid(values, lats, lons, grid)
    assert coarse.shape == (28, 82)
    assert coarse[0, 0] == pytest.approx(4.0)
    assert coarse[1, 0] == pytest.approx(2.0)


def test_cells_without_data_stay_nan():
    grid = Grid.for_bbox()
    lats, lons = native_axes(0.05)
    values = np.full((lats.size, lons.size), np.nan)
    values[0, 0] = 1.0
    coarse = bin_to_grid(values, lats, lons, grid)
    assert np.isfinite(coarse).sum() == 1


def test_front_strength_measures_change_per_10_km():
    lats, lons = native_axes(0.05)
    # 0.2 degC per native step east-west; 0.05 deg of longitude near 4N is ~5.55 km -> ~0.36 degC per km.
    sst = np.tile(np.arange(lons.size) * 0.2, (lats.size, 1)) + 28.0
    strength = front_strength(sst, lats, lons)
    row = np.argmin(np.abs(lats - 4.0))
    expected = 0.2 / (0.05 * 111.195 * math.cos(math.radians(lats[row]))) * 10
    assert strength[row, 50] == pytest.approx(expected, rel=1e-3)


def test_document_is_compact_json_with_nulls_and_fits_the_budget():
    grid = Grid.for_bbox()
    sst = np.full((grid.nlat, grid.nlon), 29.8712)
    sst[0, 0] = np.nan
    doc = build_document(
        {
            "sst_c": (sst, date(2026, 9, 24), "METOFFICE-GLO-SST-L4-NRT-OBS-SST-V2", 2),
            "chl_mg_m3": (np.full((grid.nlat, grid.nlon), 0.41234), date(2026, 9, 23), "chl-dataset", 3),
        },
        grid,
        generated=datetime(2026, 9, 25, 2, 0, tzinfo=timezone.utc),
    )
    data = encode(doc)
    parsed = json.loads(data)
    assert parsed["generated_utc"] == "2026-09-25T02:00:00Z"
    assert parsed["layers"]["sst_c"]["values"][:2] == [None, 29.87]
    assert parsed["layers"]["chl_mg_m3"]["date"] == "2026-09-23"
    assert len(parsed["layers"]["sst_c"]["values"]) == 28 * 82
    assert len(data) < 200_000


def test_encode_refuses_an_oversized_document():
    with pytest.raises(ValueError):
        encode({"values": [1.23456] * 60_000})
