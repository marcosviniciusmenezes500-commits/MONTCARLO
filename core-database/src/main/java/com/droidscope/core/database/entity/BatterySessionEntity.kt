package com.droidscope.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_sessions")
data class BatterySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimestampMs: Long,
    val endTimestampMs: Long? = null,
    val isCharging: Boolean,
    val startLevelPercent: Int,
    val endLevelPercent: Int,
    val startChargeCounterMicroAh: Int?,
    val endChargeCounterMicroAh: Int?,
    val avgTemperatureC: Double?,
    val avgPowerWatts: Double?,
    val isFastCharging: Boolean = false
)
