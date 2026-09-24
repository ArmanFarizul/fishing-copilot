package com.fishingcopilot.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TideDao {
    @Transaction
    @Query("SELECT * FROM tide_models WHERE spotId = :spotId")
    fun modelWithConstants(spotId: Long): Flow<TideModelWithConstants?>

    @Query("SELECT * FROM tide_models WHERE spotId = :spotId")
    suspend fun findModel(spotId: Long): TideModelEntity?

    @Query("DELETE FROM tide_models WHERE spotId = :spotId")
    suspend fun deleteModel(spotId: Long)

    @Insert
    suspend fun insertModel(model: TideModelEntity)

    @Insert
    suspend fun insertConstants(constants: List<TideConstantEntity>)

    /** Deleting the model cascades to its constants, so a refit never leaves stale rows behind. */
    @Transaction
    suspend fun replaceModel(model: TideModelEntity, constants: List<TideConstantEntity>) {
        deleteModel(model.spotId)
        insertModel(model)
        insertConstants(constants)
    }
}
