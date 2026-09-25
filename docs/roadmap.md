# Roadmap

Status pada 2026-09-25. MVP yang dipersetujui: pasang surut, Bite Score, cuaca laut, lubuk dan log tangkapan.

## Siap (MVP)

| Ciri | Nota |
|---|---|
| Onboarding 4 langkah | Nama, avatar 3D, gaya memancing, spesies sasaran, lubuk utama pada peta |
| Pasang surut | Model harmonik daripada Open-Meteo, berfungsi tanpa internet; laras waktu lubuk; lihat `sumber-data.md` |
| Cuaca laut | Ombak, angin, tekanan dan arus dalam bahasa biasa (skala WMO dan Beaufort), dengan animasi |
| Matahari dan bulan | Waktu disahkan dengan USNO; waktu solunar; kalendar fasa bulan 30 hari |
| Tarikh Hijrah | Takwim rasmi JAKIM; anggaran MABIMS untuk hari yang belum diterbitkan |
| Bite Score | Formula spesifikasi; graf 24 jam; waktu emas; lihat `bite-score.md` |
| Log tangkapan | Butang Strike!, isian automatik keadaan, gambar, ringkasan umpan dan air; sunting spesies, umpan, saiz, gambar dan nota (keadaan semasa Strike kekal) |
| Lubuk | Tambah pada peta, namakan semula, jadikan utama, padam |
| Bahasa | BM dan English; lihat `i18n.md` |
| Peringatan waktu emas | 45 minit sebelum setiap waktu emas; bunyi loceng dan getaran; tetapan hidup/mati, bunyi, getaran dan ujian |
| Pipeline satelit | Larian harian di GitHub (`pipeline/`); suhu, tubir suhu, klorofil-a dan kejernihan air diterbitkan ke release `latest-data` (~49 KB) |
| Kad satelit | Suhu, plankton, kejernihan dan tubir suhu terdekat bagi lubuk utama; berfungsi tanpa internet; lihat `satelit.md` |
| Peta luar talian | Tetapan: muat turun peta asas OpenFreeMap (zoom 0–10 seluruh kawasan, zoom 11–14 kira-kira 40 km sekeliling setiap lubuk) ke pangkalan data luar talian MapLibre |
| Peta satelit | Tab Lubuk, paparan Peta: petak berwarna bagi plankton, suhu, tubir suhu atau kejernihan; ketik petak untuk nilai; pin lubuk sendiri |
| Amaran MetMalaysia | Skrin sendiri, dibuka daripada satu baris di skrin utama (merah jika ada amaran untuk kawasan lubuk); teks rasmi BM/EN; dipadankan dengan negeri lubuk; lihat `sumber-data.md` bahagian 5 |

## Belum dibuat (daripada spesifikasi)

| Ciri | Keperluan / sebab tertangguh |
|---|---|
| Carta nautika (OpenSeaMap, GEBCO) | Kedalaman dan tanda pelayaran; peta asas luar talian sudah ada |
| Jadual pasang surut JUPEM | Belum diperoleh; akan digunakan untuk menyelaras ketepatan dan datum |
| Tackle Advisor penuh | Saiz ladung sudah dipaparkan pada baris arus; skrin khusus belum |
| Keselamatan marin: penggera sauh hanyut, tersadai, tebing tenggelam | Perlukan servis GPS latar belakang |
| Widget skrin utama (Glance) | Belum |
| Sunlight Mode | Belum; warna kini ditulis terus dalam kod, perlu dipindah ke tema dahulu |
| Penapis spesies dan Bait Tracker | Belum |

## Di luar spesifikasi (ditanya oleh pemilik)

| Ciri | Nota |
|---|---|
| Radar hujan | Sumber data belum dikaji; perlukan internet |
| Data paras sungai | JPS publicinfobanjir; API rasmi belum disahkan |
| Peraturan memancing / zon larangan (Taman Laut) | Sumber sempadan belum dikaji |
| Rakam laluan | Perlukan servis GPS latar belakang |
| Ukur jarak pada peta | Mudah dibina di atas peta sedia ada |
| Pemilih bahasa dalam aplikasi (Android 12 ke bawah) | Android 13+ sudah boleh pilih melalui Settings |

## Nilai yang perlu disemak oleh pemilik

- Bite Score: tiga keadaan yang tidak ditakrif dalam spesifikasi (`bite-score.md`).
- Nama Beaufort BM tahap 7 hingga 10 menggunakan istilah DBP yang kurang dikenali (`strings_marine_guide.xml`).
- Istilah "tiupan" untuk gust angin.
- Amaran MetMalaysia: garis pemisah Johor (103.6° T) dan Sabah (117.2° T), dan kawasan perkapalan yang belum dipadankan (`sumber-data.md`).
- Data satelit: tahap tubir lemah dan jalur kejernihan air ialah andaian (`satelit.md`).
