package com.droidscope.core.domain

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val processor: String,
    val ramGb: Int,
    val storageGb: Int,
    val batteryPercent: Int,
    val batteryTempC: Double,
    val isCharging: Boolean,
    val connectionStatus: String,
    val uptimeHours: Int,
)
