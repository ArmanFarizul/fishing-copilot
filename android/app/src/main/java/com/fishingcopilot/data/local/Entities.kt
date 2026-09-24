package com.fishingcopilot.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "spots")
data class SpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String? = null,
    val depthMeters: Double? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** Shift applied to predicted tide times at this spot; estuaries usually turn later than the open sea. */
    @ColumnInfo(defaultValue = "0") val tideOffsetMinutes: Int = 0
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
    // Conditions recorded automatically at the strike; null when that data was not available yet.
    /** Metres relative to the tide model's mean level. */
    val waterLevel: Double?,
    /** "RISING" or "FALLING". */
    val tideState: String?,
    /** com.fishingcopilot.astro.MoonPhaseName name. */
    val moonPhase: String?,
    val biteScore: Double?,
    val photoUri: String?,
    val timestamp: Long = System.currentTimeMillis(),
    /** Hijri date as yyyy-MM-dd (Hijri year-month-day). */
    @ColumnInfo(defaultValue = "NULL") val hijriDate: String? = null,
    @ColumnInfo(defaultValue = "NULL") val notes: String? = null
)

@Entity(tableName = "tide_cache", primaryKeys = ["stationCode", "timestamp"])
data class TideCacheEntity(
    val stationCode: String,
    val timestamp: Long,
    val predictedHeight: Double,
    val eventType: String
)
