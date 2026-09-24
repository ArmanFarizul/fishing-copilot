# SPESIFIKASI MUKTAMAD: SMART FISHING COPILOT (ANDROID NATIVE)
Versi: 4.0 | Seni Bina: 100% Offline-First & Zero-Cost

================================================================================
1. SENI BINA SISTEM SIFAR KOS (OFFLINE-FIRST)
================================================================================
Aplikasi dibina khusus untuk beroperasi di persekitaran laut terbuka, muara sungai bakau, dan pulau terpencil tanpa memerlukan capaian internet berterusan atau langganan pelayan bulanan.

Rajah Aliran Sistem:
[INPUT GPS / SPOT]
  ├──> [ANDROID NATIVE KOTLIN]
  │      • Pytides Math (Enjin Harmonik Pasang Surut Client-side)
  │      • SunCalc Offline (Solunar & Fasa Bulan Hijrah)
  │      • Room SQLite (Storan Data Tempatan)
  │      • MBTiles Offline (OpenSeaMap & GEBCO Bathymetry)
  │      • Data Pasang Surut Rasmi JUPEM (SQLite Ingest)
  ├──> [API PERCUMA DIRECT]
  │      • Open-Meteo Marine (Ombak, Angin, Suhu Air)
  │      • UNESCO IOC Sea Level (Penentukuran Aras Air Sebenar)
  └──> [PIPELINE PERCUMA]
         • GitHub Actions Cron Harian -> Muat Turun NetCDF Copernicus -> Ekstrak Klorofil-a & SST -> daily_marine_fronts.json -> Simpan di GitHub Releases

================================================================================
2. SUMBER DATA & API PERCUMA
================================================================================
1. Pasang Surut (Ramalan): Pytides / Algoritma Harmonik Tempatan (Client-Side). Mengira 365 hari berasaskan pemalar M2, S2, N2, K1, O1.
2. Pasang Surut (Masa Nyata): UNESCO IOC Sea Level REST JSON API.
   - Kod Stesen Malaysia & Sekitar: kuka (Kukup), tiom (Tioman), cema (Cendering), raff (Raffles).
   - Format Endpoint: [https://ioc-sealevelmonitoring.org/service.php?query=data&code=STATION_CODE&format=json](https://ioc-sealevelmonitoring.org/service.php?query=data&code=STATION_CODE&format=json)
3. Pasang Surut Rasmi Tempatan: Ingest jadual tahunan rasmi JUPEM ke dalam SQLite tempatan.
4. Ombak & Angin: Open-Meteo Marine API (Bebas Kunci API). Mengambil wave height, wave period, kelajuan angin, arah tiupan, dan suhu air laut.
5. Fasa Bulan & Solunar: SunCalc (Offline). Mengira fasa bulan Hijrah, waktu terbit/terbenam matahari & bulan, dan waktu makan ikan (Major/Minor periods).
6. Peta Kedalaman Laut: Jubin peta luar talian MBTiles OpenSeaMap dan GEBCO Bathymetry.
7. Data Satelit Plankton & SST: Copernicus Marine Service (CMEMS) diproses percuma via GitHub Actions.

================================================================================
3. SALURAN PAIP SATELIT SIFAR KOS (GITHUB ACTIONS)
================================================================================
Pemicu berjalan percuma setiap hari jam 02:00 UTC melalui GitHub Actions runner. Skrip Python memuat turun NetCDF kawasan Malaysia (Selat Melaka & Laut China Selatan), mengekstrak matriks Klorofil-a dan SST, memampatkannya kepada daily_marine_fronts.json (<200KB), dan memuat naik ke GitHub Releases untuk dimuat turun oleh aplikasi sekali sehari.

Fail Workflow (.github/workflows/process_copernicus.yml):
name: Process Copernicus Satellite Data
on:
  schedule:
    - cron: '0 2 * * *'
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - name: Install Dependencies
        run: pip install copernicusmarine pandas numpy
      - name: Extract & Compress Satellite Data
        env:
          COPERNICUS_USER: ${{ secrets.COPERNICUS_USER }}
          COPERNICUS_PASSWORD: ${{ secrets.COPERNICUS_PASSWORD }}
        run: python scripts/process_satellite.py
      - name: Upload Daily JSON
        uses: softprops/action-gh-release@v1
        with:
          files: daily_marine_fronts.json
          tag_name: latest-data
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

================================================================================
4. PENENTUAN PASANG SURUT TEPAT DI SPOT SPESIFIK
================================================================================
A. Formula Jarak Haversine (Mencari Stesen IOC Terdekat):
   d = 2R * arcsin(sqrt(sin^2(dlat/2) + cos(lat1) * cos(lat2) * sin^2(dlon/2)))

B. Penyelarasan Muara (Estuary Offset):
   T_spot = T_stesen + Delta_t_offset
   H_spot = Mean_Level + (H_stesen - Mean_Level) * Ratio
   - Aplikasi menyediakan slider offset manual (+15 hingga +60 minit) untuk melaraskan waktu tohor mengikut alur sungai.

C. Model Hidrodinamik Grid Global (Laut Terbuka):
   - Menggunakan pustaka pyTMD berasaskan TPXO9 atau FES2014 untuk koordinat grid lautan terbuka jauh dari pantai.

================================================================================
5. ALGORITMA SKOR AKTIVITI IKAN (BITE SCORE 1-10)
================================================================================
Formula Pemarkahan Berwajaran:
Raw Score = (F_tide * 0.35) + (F_light * 0.25) + (F_solunar * 0.25) + (F_baro * 0.15)
Bite Score = 1 + (Raw Score * 9)

Matriks Nilai Sub-Faktor (0.0 - 1.0):
1. F_tide (Aliran Pasang Surut - 35%):
   - 0.2 : Kemuncak pasang / surut maksimum (+/- 30 minit; air genang/mati).
   - 0.6 : Pertengahan kitaran perbani (arus terlalu deras, ladung hanyut).
   - 1.0 : Air mula tohor / bergerak menolak (minit 45 hingga 150 selepas puncak).
2. F_light (Waktu Cahaya - 25%):
   - 1.0 : Waktu Subuh (Fajar) & Senja (Maghrib) - crepuscular window.
   - 0.6 : Malam hari.
   - 0.3 : Tengah hari terik (11:00 AM - 3:00 PM; ikan culas).
3. F_solunar (Fasa Bulan - 25%):
   - 1.0 : Anak Bulan (1-3 Hijrah) & Bulan Penuh (14-16 Hijrah) - Air Hidup / Perbani.
   - 0.7 : Bulan sabit suku awal/akhir.
   - 0.3 : Bulan separuh (7-9 & 21-23 Hijrah) - Air Mati / Pasang Anak.
4. F_baro (Kestabilan Barometer - 15%):
   - 1.0 : Tekanan stabil 1010 - 1015 hPa (kadar ubah < 1 hPa / 3 jam).
   - 0.7 : Tekanan naik perlahan selepas ribut.
   - 0.2 : Tekanan menjunam > 3 hPa / 3 jam (petanda ribut).

================================================================================
6. KAEDAH PENGESANAN AKTIVITI IKAN DI LAPANGAN
================================================================================
1. Satelit Copernicus:
   - Klorofil-a: 0.2 - 1.5 mg/m^3 (kepekatan plankton sihat untuk menarik ikan umpan).
   - Tubir Suhu (SST Fronts): Beza suhu >= 1.0 darjah C dalam zon sempit. Suhu tropika optimum: 28°C - 30°C.
2. Struktur Bawah Air: Drop-off (tubir), unjam, tiang jeti, karang, dan eddies (pusaran arus).
3. Keadaan Air: Elakkan air teh tarik pekat banjir. Jarak nampak ideal: 1-2 meter (hijau zamrud / biru minyak).
4. Tanda Visual: Burung laut menjunam, air mereneh (baitfish boiling), garisan pertembungan air muara, bau hanyir minyak ikan.
5. Perkakasan: Castable sonar (Deeper/Garmin), kamera bawah air, smart bite alarm.

================================================================================
7. MODUL-MODUL TAMBAHAN (ADD-ON SUITE)
================================================================================
1. Garis Masa Interaktif 24-Jam: Bar warna harian berserta penggera notifikasi 45 minit sebelum Waktu Emas bermula.
2. Tackle Advisor (Kalkulator Ladung Dinamik):
   - Arus perlahan (< 0.5 knot): Ladung Saiz 2 - 4 | Perambut Running Sinker / Hanyut.
   - Arus sederhana (0.5 - 1.2 knot): Ladung Saiz 5 - 7.
   - Arus laju (> 1.2 knot): Ladung Saiz 8 - 10 atau Sauh | Perambut Apollo Rig.
3. Bait Tracker: Jadual apollo tamban/selar waktu pagi dan waktu menyuluh udang semasa air surut kering malam.
4. Penapis Spesies: Siakap (air pasang tohor muara), Pari (beting pasir malam), Tenggiri (air jernih siang berarus).
5. Keselamatan Marin: Anchor Drift Alarm (>25m), Stranding Alert (bot tersadai atas lumpur), Tide Trapped Alert (tebing tenggelam), Status Ombak Open-Meteo (Hijau <0.8m, Kuning 0.8-1.5m, Merah >1.5m).
6. Smart Log & Private Circles: Autofill koordinat, paras air, dan fasa bulan secara automatik semasa strike; analisis umpan berkesan (contoh: 70% Udang Hidup vs 30% Sotong) dalam kumpulan rakan tertutup.

================================================================================
8. IMPLEMENTASI ANDROID NATIVE (KOTLIN + ROOM SQLITE)
================================================================================
--- A. build.gradle.kts (Module :app) ---
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
}
dependencies {
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")
}

--- B. LocalDatabase.kt (Entiti Room) ---
package com.fishingcopilot.data.local
import androidx.room.*

@Entity(tableName = "spots")
data class SpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String? = null,
    val depthMeters: Double? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "catch_logs",
    foreignKeys = [
        ForeignKey(
            entity = SpotEntity::class,
            parentColumns = ["id"],
            childColumns = ["spotId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["spotId"])]
)
data class CatchLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spotId: Long?,
    val species: String,
    val weightKg: Double?,
    val lengthCm: Double?,
    val baitUsed: String?,
    val waterLevel: Double,
    val tideState: String,
    val moonPhase: String,
    val biteScore: Double,
    val photoUri: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tide_cache", primaryKeys = ["stationCode", "timestamp"])
data class TideCacheEntity(
    val stationCode: String,
    val timestamp: Long,
    val predictedHeight: Double,
    val eventType: String
)

--- C. FishingDao.kt ---
package com.fishingcopilot.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FishingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: SpotEntity): Long

    @Query("SELECT * FROM spots ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllSpotsFlow(): Flow<List<SpotEntity>>

    @Insert
    suspend fun insertCatchLog(log: CatchLogEntity): Long

    @Query("SELECT * FROM catch_logs ORDER BY timestamp DESC")
    fun getAllCatchLogsFlow(): Flow<List<CatchLogEntity>>

    @Query("SELECT * FROM catch_logs WHERE spotId = :spotId AND species LIKE '%' || :species || '%' ORDER BY timestamp DESC")
    suspend fun getLogsBySpotAndSpecies(spotId: Long, species: String): List<CatchLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTideCache(cacheList: List<TideCacheEntity>)

    @Query("SELECT * FROM tide_cache WHERE stationCode = :stationCode AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getTideForecast(stationCode: String, startTime: Long, endTime: Long): List<TideCacheEntity>
}

================================================================================
9. SISTEM REKA BENTUK UI/UX (MATERIAL 3 JETPACK COMPOSE)
================================================================================
--- A. Color.kt ---
package com.fishingcopilot.ui.theme
import androidx.compose.ui.graphics.Color

val OceanMidnight = Color(0xFF030C1B)
val OceanSurface = Color(0xFF0A192F)
val OceanCardBorder = Color(0xFF112240)
val NauticalCyan = Color(0xFF00E5FF)
val PrimeGreen = Color(0xFF00E676)
val CautionYellow = Color(0xFFFFD600)
val AlertRed = Color(0xFFFF1744)
val TextHighContrast = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF8892B0)

--- B. Type.kt ---
package com.fishingcopilot.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FishingTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )
)

--- C. Components.kt (Animasi Fungsional & Maklum Balas Deria) ---
package com.fishingcopilot.ui.components
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.fishingcopilot.ui.theme.OceanCardBorder
import com.fishingcopilot.ui.theme.PrimeGreen
import kotlin.math.sin

@Composable
fun PulsingScoreBadge(score: Double, isPrimeTime: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val scale by if (isPrimeTime) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        rememberUpdatedState(1.0f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(90.dp)
            .scale(scale)
            .background(
                color = if (isPrimeTime) PrimeGreen.copy(alpha = 0.2f) else OceanCardBorder,
                shape = CircleShape
            )
    ) {
        Text(
            text = String.format("%.1f", score),
            style = MaterialTheme.typography.displayLarge,
            color = if (isPrimeTime) PrimeGreen else Color.White
        )
    }
}

@Composable
fun AnimatedWaveBackground(waterLevelFraction: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val baseWaterHeight = height * (1f - waterLevelFraction.coerceIn(0.1f, 0.9f))
        
        val wavePath = Path().apply {
            moveTo(0f, height)
            lineTo(0f, baseWaterHeight)
            val waveLength = width / 1.2f
            val amplitude = 18.dp.toPx()
            
            var x = 0f
            while (x <= width) {
                val y = baseWaterHeight + amplitude * sin((x / waveLength * 2 * Math.PI + phase).toDouble()).toFloat()
                lineTo(x, y)
                x += 10f
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.45f), Color(0xFF0A192F).copy(alpha = 0.95f))
            )
        )
    }
}

--- D. Maklum Balas Deria & Sunlight Mode ---
- Quick Strike Snap: Getaran HapticFeedbackType.LongPress apabila butang tangkapan ditekan.
- Siren Sauh Hanyut: Getaran berterusan dan nada dering siren audio sekiranya bot berhanyut melebihi 25m.
- Tide Change Chime: Isyarat loceng marin lembut 15 minit sebelum air mula tohor menolak.
- Sunlight High-Visibility Mode: Toggle pantas yang mengubah tema skrin kepada latar belakang putih tulen (#FFFFFF), sempadan kad hitam tebal 2dp, teks hitam pekat (#000000), dan kecerahan skrin 100%.

================================================================================
10. WIDGET SKRIN UTAMA ANDROID (JETPACK GLANCE)
================================================================================
package com.fishingcopilot.widget
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*

class FishingGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceWidgetContent(
                stationName = "Kukup",
                tideState = "Mula Pasang Menolak",
                waterLevel = "2.4 m",
                biteScore = "8.5"
            )
        }
    }

    @Composable
    private fun GlanceWidgetContent(stationName: String, tideState: String, waterLevel: String, biteScore: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.End) {
                Text(text = stationName, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp))
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(text = "SKOR AIR: $biteScore/10", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            Text(text = "$tideState ($waterLevel)", style = TextStyle(fontSize = 12.sp))
        }
    }
}

================================================================================
11. ANALISIS KOS OPERASI BULANAN
================================================================================
- Pelayan Backend: RM 0.00 / bulan (Serverless via GitHub Actions Free Tier)
- Pangkalan Data: RM 0.00 / bulan (Room SQLite Terbina dalam Android OS)
- Data Pasang Surut: RM 0.00 / bulan (UNESCO IOC & Pengiraan Harmonik)
- Data Cuaca Marin: RM 0.00 / bulan (Open-Meteo Marine API Bebas Kunci)
- Data Satelit Plankton/SST: RM 0.00 / bulan (Copernicus Marine Open Data)
- Peta Kontur Kedalaman: RM 0.00 / bulan (Jubin Terbuka OpenSeaMap & GEBCO)
JUMLAH KOS OPERASI: RM 0.00 / bulan (100% Sifar Kos Berterusan)