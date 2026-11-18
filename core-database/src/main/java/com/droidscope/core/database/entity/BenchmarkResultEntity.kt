package com.droidscope.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmark_results")
data class BenchmarkResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val suiteName: String,
    val singleCoreScore: Int?,
    val multiCoreScore: Int?,
    val memoryScore: Int?,
    val storageScore: Int?,
    val gpuScore: Int? = null,
    val startTempC: Double?,
    val endTempC: Double?,
    val batteryPercent: Int,
    val detailsJson: String
)
