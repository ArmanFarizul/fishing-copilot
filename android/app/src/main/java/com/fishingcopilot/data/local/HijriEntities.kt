package com.fishingcopilot.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One day of JAKIM's official takwim. [date] is ISO yyyy-MM-dd so string ranges sort as dates. */
@Entity(tableName = "hijri_days")
data class HijriDayEntity(
    @PrimaryKey val date: String,
    val hijriYear: Int,
    val hijriMonth: Int,
    val hijriDay: Int
)

@Dao
interface HijriDao {
    @Query("SELECT * FROM hijri_days WHERE date BETWEEN :from AND :to ORDER BY date")
    fun range(from: String, to: String): Flow<List<HijriDayEntity>>

    @Query("SELECT COUNT(*) FROM hijri_days WHERE date LIKE :yearPrefix || '%'")
    suspend fun countInYear(yearPrefix: String): Int

    @Upsert
    suspend fun upsertAll(days: List<HijriDayEntity>)
}
