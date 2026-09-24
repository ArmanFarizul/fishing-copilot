# Sumber Data dan API

Semua endpoint di bawah diuji pada 2026-09-24. Tiada satu pun memerlukan API key. Hanya Copernicus Marine memerlukan akaun (username dan password).

## 1. Copernicus Marine (klorofil-a dan SST)

Akses melalui Copernicus Marine Toolbox, pustaka Python `copernicusmarine` versi 2.4.1 (Python 3.10 ke atas). Tiada REST API atau API key. Data dimuat turun oleh pipeline GitHub Actions, bukan oleh aplikasi.

**Kelayakan** dibaca daripada pembolehubah persekitaran ([dokumentasi login](https://toolbox-docs.marine.copernicus.eu/en/stable/usage/login-usage.html)):

- `COPERNICUSMARINE_SERVICE_USERNAME`
- `COPERNICUSMARINE_SERVICE_PASSWORD`

Nama ini berbeza daripada `COPERNICUS_USER` / `COPERNICUS_PASSWORD` dalam spesifikasi asal. Guna nama di atas.

**Dataset yang dipilih:**

| Data | Product ID | Dataset ID | Pemboleh ubah | Resolusi | Kemas kini |
|---|---|---|---|---|---|
| Klorofil-a | `OCEANCOLOUR_GLO_BGC_L4_NRT_009_102` | `cmems_obs-oc_glo_bgc-plankton_nrt_l4-gapfree-multi-4km_P1D` | `CHL` (mg/m³) | 4 km, harian | Lewat kira-kira 2 hari |
| Suhu permukaan laut | `SST_GLO_SST_L4_NRT_OBSERVATIONS_010_001` | `METOFFICE-GLO-SST-L4-NRT-OBS-SST-V2` | `analysed_sst` (Kelvin) | 0.05°, harian | Lewat kira-kira 1 hari |
| Kejernihan air (pilihan) | `OCEANCOLOUR_GLO_BGC_L4_NRT_009_102` | `cmems_obs-oc_glo_bgc-transp_nrt_l4-gapfree-multi-4km_P1D` | `ZSD` (kedalaman Secchi, m) | 4 km, harian | Lewat kira-kira 2 hari |

Nota:
- Dataset "gapfree" diisi secara interpolasi, jadi tiada lubang akibat awan. Ini penting di perairan tropika yang kerap berawan.
- Dataset NRT klorofil hanya menyimpan kira-kira 18 hari terkini (5 hingga 22 September 2026 semasa diuji). Pipeline harian sudah memadai; sejarah panjang tidak tersedia daripada dataset ini.
- SST dalam Kelvin. Tolak 273.15 untuk Celsius.
- `ZSD` (kedalaman Secchi) boleh terus dipadankan dengan panduan "jarak nampak ideal 1 hingga 2 meter" dalam spesifikasi bahagian 6.

**Kawasan (bounding box) cadangan:** latitud 0.5 hingga 7.5 U, longitud 99 hingga 119.5 T. Kawasan ini meliputi Selat Melaka, Laut China Selatan, Sabah dan Sarawak. Pada resolusi 4 km, ia bersamaan kira-kira 80,000 titik bagi setiap pemboleh ubah, jadi data mesti diringkaskan (contohnya hanya zon tubir suhu dan klorofil) untuk mencapai sasaran JSON di bawah 200KB.

## 2. Open-Meteo Marine (ombak, arus, aras laut)

```
https://marine-api.open-meteo.com/v1/marine?latitude=1.325&longitude=103.44&hourly=wave_height,wave_period,wave_direction,sea_surface_temperature,ocean_current_velocity,ocean_current_direction,sea_level_height_msl&timezone=Asia/Kuala_Lumpur
```

| Pemboleh ubah | Unit | Kegunaan |
|---|---|---|
| `wave_height`, `wave_period`, `wave_direction` | m, s, ° | Status ombak (hijau/kuning/merah) |
| `sea_surface_temperature` | °C | SST titik spot |
| `ocean_current_velocity`, `ocean_current_direction` | km/j, ° | Tackle Advisor (1 knot = 1.852 km/j) |
| `sea_level_height_msl` | m | Aras laut termasuk pasang surut, sebagai sandaran di kawasan tanpa stesen IOC |

Angin **tiada** dalam API Marine, walaupun spesifikasi asal menyatakan sebaliknya. Ambil dari API Forecast di bawah.

## 3. Open-Meteo Forecast (angin dan tekanan)

```
https://api.open-meteo.com/v1/forecast?latitude=1.325&longitude=103.44&hourly=pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m&timezone=Asia/Kuala_Lumpur
```

`pressure_msl` (hPa) ialah input untuk faktor barometer (`F_baro`) dalam Bite Score. Faktor ini tiada sumber data dalam spesifikasi asal.

Open-Meteo percuma untuk kegunaan bukan komersial dan memerlukan atribusi. Lihat syarat di https://open-meteo.com/en/terms.

## 4. UNESCO IOC Sea Level Monitoring (aras air sebenar)

```
https://ioc-sealevelmonitoring.org/service.php?query=data&code=ms002&timestart=2026-09-24T00:00&timestop=2026-09-24T02:00&format=json
```

Respons: `[{"slevel":1.046,"stime":"2026-09-24 00:00:00","sensor":"prs"}, ...]` (meter, masa UTC).

**Kod stesen dalam spesifikasi asal (`kuka`, `tiom`, `cema`, `raff`) tidak wujud.** Stesen Malaysia yang sebenar dan aktif:

| Kod | Lokasi | Koordinat | Sensor |
|---|---|---|---|
| `lank` | Langkawi | 6.416, 99.767 | radar |
| `ms001` | Porto Malai, Langkawi | 6.257, 99.734 | tekanan |
| `ms002` | Kerachut, Pulau Pinang | 5.451, 100.182 | tekanan |
| `ms003` | Pulau Perak | 5.684, 98.939 | tekanan |
| `ms004` | Pulau Perhentian | 5.933, 102.767 | tekanan |
| `ms005` | Kudat, Sabah | 6.878, 116.846 | tekanan |
| `ms006` | Lahad Datu, Sabah | 5.076, 119.078 | tekanan |

Tiada stesen IOC di selatan Semenanjung (Johor, Melaka, Tioman) atau Sarawak. Stesen Tanjong Pagar (Singapura, `tanjo`) sudah tidak aktif sejak 2015. Untuk kawasan ini, aplikasi bergantung pada jadual JUPEM dan `sea_level_height_msl` daripada Open-Meteo.

Senarai penuh stesen: `https://ioc-sealevelmonitoring.org/service.php?query=stationlist&showall=all`
