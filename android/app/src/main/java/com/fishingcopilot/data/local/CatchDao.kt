package com.fishingcopilot.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SpotCatchCount(val spotId: Long, val count: Int)

@Dao
interface CatchDao {
    @Insert
    suspend fun insert(log: CatchLogEntity): Long

    @Update
    suspend fun update(log: CatchLogEntity)

    @Delete
    suspend fun delete(log: CatchLogEntity)

    @Query("SELECT * FROM catch_logs ORDER BY timestamp DESC")
    fun allFlow(): Flow<List<CatchLogEntity>>

    /** Most recently used distinct baits, for quick-pick chips. */
    @Query(
        "SELECT baitUsed FROM catch_logs WHERE baitUsed IS NOT NULL AND baitUsed != '' " +
            "GROUP BY baitUsed ORDER BY MAX(timestamp) DESC LIMIT :limit"
    )
    fun recentBaitsFlow(limit: Int): Flow<List<String>>

    @Query("SELECT spotId, COUNT(*) AS count FROM catch_logs WHERE spotId IS NOT NULL GROUP BY spotId")
    fun countsBySpotFlow(): Flow<List<SpotCatchCount>>
}
