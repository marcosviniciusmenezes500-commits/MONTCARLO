package com.droidscope.core.domain

data class SensorInfo(
    val name: String,
    val vendor: String,
    val type: String,
    val resolution: Float,
    val maximumRange: Float,
    val power: Float,
    val version: Int
)

data class CpuCoreInfo(
    val core: Int,
    val currentFrequencyMhz: Int?,
    val maximumFrequencyMhz: Int?
)
