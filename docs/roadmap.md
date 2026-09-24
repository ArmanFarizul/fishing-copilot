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
| Log tangkapan | Butang Strike!, isian automatik keadaan, gambar, ringkasan umpan dan air |
| Lubuk | Tambah pada peta, namakan semula, jadikan utama, padam |
| Bahasa | BM dan English; lihat `i18n.md` |
| Peringatan waktu emas | 45 minit sebelum setiap waktu emas; bunyi loceng dan getaran; tetapan hidup/mati, bunyi, getaran dan ujian |

## Belum dibuat (daripada spesifikasi)

| Ciri | Keperluan / sebab tertangguh |
|---|---|
| Pipeline satelit: larian sebenar | Pipeline sudah dibina dan diuji di GitHub (`pipeline/`); menunggu GitHub Secrets Copernicus untuk muat turun pertama |
| Paparan data satelit dalam aplikasi | Klorofil-a, tubir suhu dan kejernihan air daripada `daily_marine_fronts.json`; belum dibina |
| Peta luar talian (MBTiles) dan carta nautika (OpenSeaMap, GEBCO) | Peta kini perlukan internet; OpenFreeMap menyediakan MBTiles mingguan |
| Jadual pasang surut JUPEM | Belum diperoleh; akan digunakan untuk menyelaras ketepatan dan datum |
| Tackle Advisor penuh | Saiz ladung sudah dipaparkan pada baris arus; skrin khusus belum |
| Keselamatan marin: penggera sauh hanyut, tersadai, tebing tenggelam | Perlukan servis GPS latar belakang |
| Amaran MetMalaysia | API diuji (`sumber-data.md`); belum dipaparkan |
| Widget skrin utama (Glance) | Belum |
| Sunlight Mode | Belum; warna kini ditulis terus dalam kod, perlu dipindah ke tema dahulu |
| Penapis spesies dan Bait Tracker | Belum |
| Suntingan rekod tangkapan | Kini hanya tambah dan padam |

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
