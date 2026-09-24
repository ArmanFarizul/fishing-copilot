# Bite Score

Skor 1 hingga 10 yang menganggarkan betapa sesuai sesuatu masa untuk memancing di lubuk utama. Formula datang daripada spesifikasi (`spesifikasi.md`, bahagian 5). Kod: `android/app/src/main/java/com/fishingcopilot/bite/`.

```
raw   = 0.35 x F_tide + 0.25 x F_light + 0.25 x F_solunar + 0.15 x F_baro
score = 1 + 9 x raw
```

Skor dikira untuk masa sekarang dan setiap 15 minit bagi 24 jam akan datang. "Waktu emas" ialah skor 7.5 ke atas; kad memaparkan tempoh berterusan yang mengandungi skor tertinggi.

## Faktor

| Faktor | Keadaan | Nilai | Sumber |
|---|---|---|---|
| Pasang surut (35%) | Dalam 30 minit sebelum atau selepas air pasang/surut (air genang) | 0.2 | Spesifikasi |
| | 45 hingga 150 minit selepas air bertukar | 1.0 | Spesifikasi |
| | Masa lain, termasuk 30 hingga 45 minit selepas bertukar | 0.6 | Spesifikasi ("pertengahan kitaran") |
| Cahaya (25%) | Terang tanah hingga sejam selepas matahari terbit; sejam sebelum terbenam hingga akhir senja | 1.0 | Spesifikasi (Subuh dan Maghrib), tempoh sejam ialah tafsiran |
| | Malam | 0.6 | Spesifikasi |
| | 11:00 hingga 15:00 | 0.3 | Spesifikasi |
| | Siang di luar waktu di atas | 0.6 | **Tidak ditakrif dalam spesifikasi**; disamakan dengan malam |
| Bulan (25%) | Hari Hijrah 1-3 dan 14-16 (air besar) | 1.0 | Spesifikasi |
| | Hari Hijrah 7-9 dan 21-23 (air mati) | 0.3 | Spesifikasi |
| | Hari lain | 0.7 | Spesifikasi ("bulan sabit") |
| Tekanan (15%) | Stabil (berubah kurang 1 hPa dalam 3 jam) pada 1010-1015 hPa | 1.0 | Spesifikasi |
| | Stabil tetapi di luar 1010-1015 hPa | 0.8 | **Tidak ditakrif**; pilihan pertengahan |
| | Naik | 0.7 | Spesifikasi |
| | Turun 1 hingga 3 hPa dalam 3 jam | 0.5 | **Tidak ditakrif**; pilihan pertengahan |
| | Turun lebih 3 hPa dalam 3 jam | 0.2 | Spesifikasi |

## Data yang tiada

Jika sesuatu faktor tiada data (contohnya tekanan sebelum ramalan cuaca pertama dimuat turun, atau model pasang surut belum siap), faktor itu tidak dikira dan pemberat faktor lain dilaraskan supaya jumlahnya kekal 100%. Kad memaparkan "Belum ada data" pada faktor tersebut.

## Sumber data

- Pasang surut: model harmonik lubuk, termasuk laras waktu lubuk (lihat `sumber-data.md`).
- Cahaya: waktu matahari dikira dalam telefon (commons-suncalc, disahkan dengan USNO).
- Bulan: tarikh Hijrah rasmi JAKIM; anggaran MABIMS untuk hari yang belum diterbitkan.
- Tekanan: ramalan `pressure_msl` Open-Meteo yang disimpan oleh kad cuaca laut.

Nilai yang ditanda **tidak ditakrif** boleh diubah dalam `BiteScore.kt` tanpa menyentuh bahagian lain.
