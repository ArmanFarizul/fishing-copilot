package com.fishingcopilot.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Last downloaded marine forecast for a spot, kept whole so the card still works offline. */
@Entity(
    tableName = "marine_forecasts",
    foreignKeys = [
        ForeignKey(entity = SpotEntity::class, parentColumns = ["id"], childColumns = ["spotId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MarineForecastEntity(
    @PrimaryKey val spotId: Long,
    val fetchedAtMillis: Long,
    /** JSON list of com.fishingcopilot.marine.MarineHour. */
    val hoursJson: String
)

@Dao
interface MarineDao {
    @Query("SELECT * FROM marine_forecasts WHERE spotId = :spotId")
    fun forecastFlow(spotId: Long): Flow<MarineForecastEntity?>

    @Query("SELECT fetchedAtMillis FROM marine_forecasts WHERE spotId = :spotId")
    suspend fun fetchedAt(spotId: Long): Long?

    @Upsert
    suspend fun upsert(forecast: MarineForecastEntity)
}
