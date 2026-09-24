package com.fishingcopilot.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SpotEntity::class,
        CatchLogEntity::class,
        TideCacheEntity::class,
        TideModelEntity::class,
        TideConstantEntity::class,
        MarineForecastEntity::class,
        HijriDayEntity::class
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class FishingDatabase : RoomDatabase() {
    abstract fun fishingDao(): FishingDao
    abstract fun tideDao(): TideDao
    abstract fun marineDao(): MarineDao
    abstract fun hijriDao(): HijriDao

    companion object {
        @Volatile
        private var instance: FishingDatabase? = null

        fun get(context: Context): FishingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FishingDatabase::class.java,
                    "fishing_copilot.db"
                ).build().also { instance = it }
            }
    }
}
