package com.droidscope.core.domain

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val processor: String,

    val ramGb: Int,
    val ramUsedGb: Double,
    val ramAvailableGb: Double,

    val storageGb: Int,
    val storageUsedGb: Double,
    val storageAvailableGb: Double,

    val batteryPercent: Int,
    val batteryTempC: Double,
    val batteryVoltageV: Double,
    val isCharging: Boolean,

    val connectionStatus: String,
    val uptimeHours: Int,

    // Hardware
    val socModel: String = "Indisponível",
    val hardware: String = "Indisponível",
    val cpuArchitecture: String = "Indisponível",
    val supportedAbis: List<String> = emptyList(),
    val cpuCoresInfo: List<CpuCoreInfo> = emptyList(),

    // Tela
    val displayResolution: String = "Indisponível",
    val densityDpi: Int = 0,
    val refreshRateHz: Float = 0f,

    // Sistema
    val kernelVersion: String = "Indisponível",
    val securityPatch: String = "Indisponível",

    // Sensores
    val sensors: List<SensorInfo> = emptyList()
)
