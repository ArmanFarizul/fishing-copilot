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

### Kualiti data dan ketepatan (diuji 2026-09-24, data 9 hingga 22 September 2026)

- **Bacaan yang hilang dilaporkan sebagai `0`, bukan kosong.** Buang nilai `slevel == 0` sebelum menggunakan data.
- **Liputan data tidak menentu.** Kerachut (`ms002`) hanya ada 108 daripada 336 jam dengan bacaan sah. Kudat (`ms005`) tiada bacaan langsung dalam tempoh itu.
- **Perbandingan dengan `sea_level_height_msl` Open-Meteo (selepas purata dibuang):**

| Stesen | Korelasi | Julat sebenar | Julat Open-Meteo |
|---|---|---|---|
| Kerachut, Pulau Pinang (`ms002`) | 0.81 | 2.03 m | 1.57 m |
| Pulau Perhentian (`ms004`) | -0.20 | 0.56 m | 1.51 m |

Di Pulau Pinang, Open-Meteo mengikut corak pasang surut dengan baik tetapi julatnya kira-kira 25% lebih kecil. Di Perhentian, kedua-dua sumber tidak sepadan. Belum dipastikan sama ada sensor IOC atau model Open-Meteo yang salah. Kesimpulan: Open-Meteo tidak boleh dianggap tepat tanpa disahkan dengan sumber rasmi (JUPEM).

Open-Meteo menyediakan `sea_level_height_msl` untuk ramalan kira-kira 10 hari dan data sejarah sekurang-kurangnya setahun ke belakang (`start_date`/`end_date`).

## 5. MetMalaysia melalui data.gov.my (amaran cuaca dan laut)

```
https://api.data.gov.my/weather/warning/
```

Percuma, tanpa kunci. Setiap amaran ada teks dalam BM dan English (`heading_bm`, `text_bm`, `heading_en`, `text_en`) serta tempoh sah (`valid_from`, `valid_to`). Jenis amaran yang dilihat semasa diuji: "Strong Winds and Rough Seas Warning", "Thunderstorms Warning" dan nasihat siklon tropika ("No Advisory" jika tiada).

Kegunaan: skrin Amaran, dibuka daripada satu baris di skrin utama (`warnings/MetWarnings.kt`, `ui/home/WarningsScreen.kt`). Aplikasi menyemak setiap minit semasa skrin utama atau skrin Amaran dipaparkan, dan memuat turun jika salinan lebih 30 minit.

Struktur yang dilihat pada 2026-09-25:

- Tarikh tanpa zon waktu; ia waktu Malaysia.
- Buletin marin yang sama disenaraikan beberapa kali, satu bagi setiap tempoh sah. Buletin dikira aktif jika mana-mana tempohnya belum tamat.
- Buletin marin mengandungi "SECTION A" (perairan Malaysia, dalam 24 batu nautika) dan "SECTION B" (perkapalan). Setiap satu ada amaran bernombor `1)`, `2)`.
- Kawasan disenaraikan selepas "waters of" atau "states of" dan dipisahkan dengan `•`, contohnya "West Johor", "Western Sabah", "Selangor (Klang and Kuala Langat)".
- "No Advisory" bermaksud tiada siklon tropika.

Padanan dengan lubuk: negeri lubuk diambil daripada kawasan pesisir terdekat (`CoastalArea`). Johor dibahagi pada 103.6° T (barat/timur), Sabah pada 117.2° T. Labuan turut dipadankan dengan "Western Sabah", kerana versi BM menulis "Sabah Barat dan Labuan". Amaran yang kawasannya tidak dapat dibaca dipaparkan untuk semua lubuk.

Had yang diketahui:
- Kawasan perkapalan dipadankan dengan lubuk (keputusan pemilik 2026-09-25). MetMalaysia tidak menerbitkan sempadannya (disemak 2026-09-25 di met.gov.my/en/forecast/marine/shipping), jadi padanan ini ialah **andaian kami** yang memilih untuk memaparkan amaran jika ragu:

  | Negeri lubuk | Kawasan perkapalan |
  |---|---|
  | Perlis, Kedah, Pulau Pinang | Phuket, Selat Melaka Utara |
  | Perak | Selat Melaka Utara |
  | Selangor, Negeri Sembilan | Selat Melaka Utara dan Selatan (sempadan kedua-duanya tidak diketahui) |
  | Melaka, Johor barat | Selat Melaka Selatan |
  | Johor timur, Pahang | Tioman |
  | Terengganu, Kelantan | Samui |
  | Sarawak barat (bawah 112° T) | Bunguran |
  | Sarawak timur | Labuan, Reef South |
  | Labuan, Sabah barat | Labuan |
  | Sabah timur | Sulu; ditambah Sulawesi di selatan 5° U (Semporna) |

  Nama dipadankan secara mengandungi, jadi "Southern part of Condore" atau "Northern part of Phuket" turut dikira. Condore, Reef North, Layang-Layang dan Palawan tidak dipadankan dengan mana-mana lubuk.
- Tarikh tamat dalam teks item (contohnya "until 9:00 AM") tidak dibaca. Item kekal selagi buletinnya aktif.
- Lesen data.gov.my belum disahkan. Halaman terma tidak dapat dibuka semasa diuji, jadi aplikasi menyebut sumber sebagai "MetMalaysia via data.gov.my".

Nota: URL mesti berakhir dengan `/`. Tanpanya, pelayan membalas dengan redirect 301.

## Checklist

Status pada 2026-09-24.

### Sudah ada

- [x] **Open-Meteo Marine:** endpoint dan pemboleh ubah diuji (ombak, arus, SST, aras laut).
- [x] **Open-Meteo Forecast:** endpoint diuji (angin, tiupan, tekanan `pressure_msl`).
- [x] **UNESCO IOC:** endpoint data diuji. Senarai 7 stesen Malaysia yang sebenar disahkan.
- [x] **Copernicus Marine:** ID dataset klorofil-a, SST dan kejernihan air disahkan daripada katalog (Toolbox 2.4.1).
- [x] **MetMalaysia:** endpoint amaran diuji.

### Belum ada

Tindakan anda:
- [x] **GitHub Secrets Copernicus:** `COPERNICUSMARINE_SERVICE_USERNAME` dan `COPERNICUSMARINE_SERVICE_PASSWORD` dimasukkan pada 2026-09-25.
- [ ] **Jadual pasang surut JUPEM:** belum diperoleh. Format, harga dan syarat penggunaannya juga belum disemak.
- [ ] **Senarai spot memancing:** nama dan koordinat.

Pembangunan:
- [x] **Ujian muat turun Copernicus sebenar:** larian pertama pipeline berjaya pada 2026-09-25 (data 23 September).
- [x] **Pipeline satelit:** `pipeline/` dan `.github/workflows/process_copernicus.yml`; grid 0.25°, kira-kira 49 KB.
- [ ] **Pemalar harmonik pasang surut** untuk ramalan 365 hari tanpa internet. Sumbernya belum ditentukan. Pilihan: TPXO atau FES (perlu pendaftaran), analisis data IOC (7 stesen sahaja), atau jadual JUPEM.
- [ ] **Peta luar talian:** sumber peta asas, lapisan OpenSeaMap, grid kedalaman GEBCO, cara menjana MBTiles, dan pustaka peta Android belum dipilih.
- [ ] **Matahari, bulan dan solunar:** dikira dalam aplikasi, jadi tiada API diperlukan. Pustaka Kotlin belum dipilih.
- [ ] **Tarikh Hijrah:** dikira dalam aplikasi. Kaedah belum dipilih dan belum disahkan.
- [x] **Atribusi Open-Meteo dan Copernicus** dalam aplikasi (Copernicus: "Generated using E.U. Copernicus Marine Service Information", mengikut lesen untuk produk terbitan).

## Enjin ramalan pasang surut: keputusan pengesahan

Diuji pada 2026-09-24. Enjin harmonik (`android/app/src/main/java/com/fishingcopilot/tide/`) dilatih dengan 8,760 jam data `sea_level_height_msl` Open-Meteo (20 September 2025 hingga 19 September 2026). Ramalannya kemudian dibandingkan dengan data yang tidak digunakan semasa latihan:

| Spot | Rujukan | Korelasi | Ralat (RMSE, selepas purata dibuang) |
|---|---|---|---|
| Kukup | Open-Meteo, 14 hari seterusnya | 0.998 | 0.05 m |
| Pulau Pinang | Open-Meteo, 14 hari seterusnya | 0.995 | 0.05 m |
| Pulau Pinang | Pengukuran sebenar IOC `ms002` (56 jam sah) | 0.85 | 0.24 m |

- Pengiraan (fit) setahun data mengambil 12 hingga 54 ms pada JVM.
- Enjin menghasilkan semula Open-Meteo hampir tepat. Jurang yang tinggal dengan pengukuran sebenar datang daripada model Open-Meteo sendiri: julat pasang surutnya kira-kira 20% lebih kecil di Pulau Pinang.
- **Datum berbeza.** Open-Meteo memberi ketinggian relatif kepada aras laut min (MSL). Jadual JUPEM dan tolok IOC menggunakan datum carta atau sifar tolok, jadi nombor ketinggian tidak boleh dibandingkan terus. Masa pasang dan surut pula boleh dibandingkan.
- Konstituen utama: Kukup M2 0.87 m, S2 0.39 m, O1 0.29 m dan K1 0.29 m (pasang surut campuran, kebanyakannya separuh harian). Pulau Pinang M2 0.50 m, S2 0.29 m dan K1 0.21 m.

## 6. Cuaca dan waktu solat di lokasi semasa

Diuji pada 2026-09-25.

- **Open-Meteo Forecast** (`api.open-meteo.com/v1/forecast`): `current` (suhu, suhu terasa, `weather_code`, indeks UV, angin), `hourly` (suhu, `precipitation_probability`, `weather_code`) dan `daily` (7 hari) dengan `timezone=auto`. Kod cuaca ikut WMO 4677. Salinan disimpan dan dimuat semula selepas 30 minit atau jika bergerak lebih 5 km.
- **Zon JAKIM daripada koordinat:** JAKIM tiada carian ini, jadi aplikasi guna `api.waktusolat.app/zones/{lat}/{lon}` (projek komuniti). Di laut ia tiada zon, jadi aplikasi guna zon pekan pantai terdekat (`CoastalArea.prayerZone`, disemak 2026-09-25). Lebih 400 km dari pantai Malaysia, waktu solat tidak dipaparkan.
- **Waktu solat:** terus daripada JAKIM e-Solat (`esolatApi/TakwimSolat&period=month&zone=`), sebulan setiap muat turun, disimpan untuk luar talian.
- **Nama tempat:** `Geocoder` Android (perlukan internet pada kebanyakan telefon); jika tiada, "Berhampiran" kawasan pesisir terdekat.

## 7. Carta nautika dan carian tempat

Diuji pada 2026-09-25.

- **Kedalaman:** GEBCO WMS `https://wms.gebco.net/mapserv`, lapisan `gebco_latest_2`, `CRS=EPSG:3857` (disokong mengikut GetCapabilities). Syarat: bukan untuk navigasi; atribusi "GEBCO Compilation Group". Grid kira-kira 450 m, jadi lorekan kasar di pesisir dan turut mewarnakan darat.
- **Tanda laut:** OpenSeaMap `https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png` (zum 9 ke atas). Data daripada OpenStreetMap (ODbL). Halaman syarat penggunaan jubin tidak ditemui; atribusi "OpenSeaMap contributors".
- **Carian tempat:** Nominatim `nominatim.openstreetmap.org/search`. Polisi (operations.osmfoundation.org/policies/nominatim): maksimum 1 permintaan sesaat, User-Agent sendiri, hasil disimpan dalam cache, **tiada autocomplete**. Aplikasi hanya mencari apabila butang ditekan.
- Kedua-dua lapisan carta tidak dimasukkan dalam muat turun peta luar talian; MapLibre hanya menyimpan jubin yang pernah dipaparkan.
