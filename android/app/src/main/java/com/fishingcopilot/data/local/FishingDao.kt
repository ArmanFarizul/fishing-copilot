package com.fishingcopilot.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
