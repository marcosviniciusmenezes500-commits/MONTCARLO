package com.droidscope.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_samples")
data class SystemSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val ramUsedPercent: Double,
    val storageFreePercent: Double,
    val avgCpuFreqMhz: Int?
)
