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
| Log tangkapan | Butang Strike!, isian automatik keadaan, gambar, ringkasan umpan dan air; sunting spesies, umpan, saiz, gambar dan nota (keadaan semasa Strike kekal); penapis tempoh (tahun ini, tahun lepas, semua, tahun lain atau julat tarikh), ringkasan ikut tempoh, pengepala bulan melekat dan pemegang tatal pantas; indeks `timestamp` (pangkalan data v7); penapis spesies dan Bait Tracker (umpan disusun ikut bilangan tangkapan, purata dan berat terbesar) |
| Lubuk | Tambah pada peta, namakan semula, jadikan utama, padam |
| Bahasa | BM dan English; lihat `i18n.md` |
| Peringatan waktu emas | 45 minit sebelum setiap waktu emas; bunyi loceng dan getaran; tetapan hidup/mati, bunyi, getaran dan ujian |
| Loceng air bertukar | 15 minit sebelum setiap pasang penuh dan surut penuh di lubuk utama (termasuk laras waktu lubuk); saluran notifikasi sendiri |
| Pipeline satelit | Larian harian di GitHub (`pipeline/`); suhu, tubir suhu, klorofil-a dan kejernihan air diterbitkan ke release `latest-data` (~49 KB) |
| Kad satelit | Suhu, plankton, kejernihan dan tubir suhu terdekat bagi lubuk utama; berfungsi tanpa internet; lihat `satelit.md` |
| Peta luar talian | Tetapan: muat turun peta asas OpenFreeMap (zoom 0–10 seluruh kawasan, zoom 11–14 kira-kira 40 km sekeliling setiap lubuk) ke pangkalan data luar talian MapLibre |
| Peta satelit | Tab Lubuk, paparan Peta: petak berwarna bagi plankton, suhu, tubir suhu atau kejernihan; ketik petak untuk nilai dan koordinat (darjah-minit dan perpuluhan); pin lubuk sendiri |
| Lokasi anda dan lubuk pilihan | Skrin utama dua bahagian: GPS (nama tempat, cuaca semasa, ramalan 12 jam dan 7 hari yang boleh dilipat, amaran ribut petir dan hujan lebat dalam 3 jam, waktu solat JAKIM ikut zon, jarak ke lubuk) dan lubuk pilihan (pilih mana-mana lubuk; bertukar sendiri jika anda dalam 2 km dari lubuk); amaran MetMalaysia kini ikon loceng |
| Sunlight Mode | Suis dalam Tetapan (bahagian Paparan); latar putih, sempadan kad hitam 2 dp, teks hitam, kecerahan skrin penuh semasa aplikasi dibuka; semua warna kini melalui satu palet (`ui/theme/Color.kt`) |
| Kawalan peta | Butang + / −, butang ke lubuk utama, dan pergi ke koordinat (perpuluhan, darjah-minit atau darjah-minit-saat; huruf N/S/E/W atau U/S/T/B) pada kedua-dua peta; berfungsi tanpa internet |
| Amaran MetMalaysia | Skrin sendiri, dibuka daripada satu baris di skrin utama (merah jika ada amaran untuk kawasan lubuk); teks rasmi BM/EN; dipadankan dengan negeri lubuk; lihat `sumber-data.md` bahagian 5 |

## Belum dibuat (daripada spesifikasi)

| Ciri | Keperluan / sebab tertangguh |
|---|---|
| Carta nautika (OpenSeaMap, GEBCO) | Kedalaman dan tanda pelayaran; peta asas luar talian sudah ada |
| Jadual pasang surut JUPEM | Belum diperoleh; akan digunakan untuk menyelaras ketepatan dan datum |
| Tackle Advisor penuh | Saiz ladung sudah dipaparkan pada baris arus; skrin khusus belum |
| Keselamatan marin: penggera sauh hanyut, tersadai, tebing tenggelam | Perlukan servis GPS latar belakang |
| Widget skrin utama (Glance) | Belum |

## Di luar spesifikasi (ditanya oleh pemilik)

| Ciri | Nota |
|---|---|
| Radar hujan | Sumber data belum dikaji; perlukan internet |
| Data paras sungai | JPS publicinfobanjir; API rasmi belum disahkan |
| Peraturan memancing / zon larangan (Taman Laut) | Sumber sempadan belum dikaji |
| Rakam laluan | Perlukan servis GPS latar belakang |
| Ukur jarak pada peta | Mudah dibina di atas peta sedia ada |
| Carian nama tempat | Perlukan internet dan servis luar (Nominatim, had 1 carian sesaat); ditangguh |
| Pemilih bahasa dalam aplikasi (Android 12 ke bawah) | Android 13+ sudah boleh pilih melalui Settings |

## Nilai yang perlu disemak oleh pemilik

- Padanan kawasan perkapalan MetMalaysia dengan lubuk ialah andaian (`sumber-data.md` bahagian 5); sempadan rasmi belum ditemui.

Diputuskan pada 2026-09-25: nilai Bite Score yang tidak ditakrif dikekalkan; nama Beaufort BM tahap 7-10 ditukar kepada istilah harian; garis pemisah Johor dan Sabah dikekalkan; tahap tubir lemah dan jalur kejernihan air dikekalkan; gust angin dalam BM ditulis "gust".
