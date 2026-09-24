package com.fishingcopilot.data.local

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
