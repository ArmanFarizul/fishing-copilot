# Data satelit dalam aplikasi

Kad "Satelit" di skrin utama membaca `daily_marine_fronts.json` daripada release `latest-data` (lihat `pipeline/README.md`). Fail disimpan dalam aplikasi, jadi kad tetap berfungsi tanpa internet. Aplikasi menyemak fail baharu setiap 6 jam.

## Cara nilai lubuk dipilih

- Grid 0.25° (kira-kira 28 km). Nilai diambil daripada petak lubuk itu sendiri.
- Jika petak itu kosong (darat atau awan), nilai diambil daripada petak berdata terdekat dalam 30 km.
- Tubir suhu: petak terdekat dalam 50 km yang mencapai tahap lemah atau kuat. Jarak diukur ke tengah petak dan dibundarkan kepada 5 km.
- Tarikh yang dipaparkan ialah tarikh lapisan paling lama antara yang ditunjukkan.
- Amaran "mungkin lapuk" muncul jika data lebih 4 hari. Kelewatan biasa ialah 1 hingga 2 hari.

## Nilai ambang

| Lapisan | Rendah | Sasaran | Tinggi | Sumber |
|---|---|---|---|---|
| Suhu permukaan | < 28 °C | 28–30 °C | > 30 °C | Spesifikasi |
| Klorofil-a | < 0.2 mg/m³ | 0.2–1.5 mg/m³ | > 1.5 mg/m³ | Spesifikasi |
| Kejernihan (kedalaman Secchi) | < 5 m keruh | 5–15 m sederhana | ≥ 15 m jernih | **Andaian kami** |
| Tubir suhu | 0.5–1.0 °C per 10 km lemah | ≥ 1.0 °C per 10 km kuat | | Kuat: spesifikasi. Lemah: **andaian kami** |

## Perlu disemak oleh pemilik

- **Tubir lemah (0.5 °C per 10 km).** Spesifikasi hanya menyatakan ≥ 1.0 °C. Data SST OSTIA dilicinkan, jadi pada 23 September 2026 tiada satu petak pun mencapai 1.0 (paling tinggi 0.97). Hanya 19 daripada 1,577 petak mencapai 0.5. Tanpa tahap lemah, kad hampir sentiasa menunjukkan "tiada tubir".
- **Jalur kejernihan air** tiada dalam spesifikasi. Pada data yang sama: 110 petak keruh, 308 sederhana, 1,192 jernih.
