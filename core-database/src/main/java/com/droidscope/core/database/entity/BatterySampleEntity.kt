package com.droidscope.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_samples")
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val levelPercent: Int,
    val temperatureC: Double?,
    val voltageV: Double?,
    val currentMicroAmps: Int?,
    val chargeCounterMicroAh: Int?,
    val isCharging: Boolean
)
