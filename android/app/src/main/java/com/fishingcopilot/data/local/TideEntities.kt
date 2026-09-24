package com.fishingcopilot.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

/** One fitted harmonic tide model per spot. See com.fishingcopilot.tide.TideModel. */
@Entity(
    tableName = "tide_models",
    foreignKeys = [
        ForeignKey(entity = SpotEntity::class, parentColumns = ["id"], childColumns = ["spotId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class TideModelEntity(
    @PrimaryKey val spotId: Long,
    val epochMillis: Long,
    val meanLevel: Double,
    val fittedAtMillis: Long
)

@Entity(
    tableName = "tide_constants",
    primaryKeys = ["spotId", "constituent"],
    foreignKeys = [
        ForeignKey(entity = TideModelEntity::class, parentColumns = ["spotId"], childColumns = ["spotId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class TideConstantEntity(
    val spotId: Long,
    val constituent: String,
    val amplitude: Double,
    val phaseDegrees: Double
)

data class TideModelWithConstants(
    @Embedded val model: TideModelEntity,
    @Relation(parentColumn = "spotId", entityColumn = "spotId") val constants: List<TideConstantEntity>
)
