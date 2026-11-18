package com.droidscope.feature.hardware

data class HardwareFeatureSpec(
    val id: String = "hardware",
    val title: String = "Hardware",
    val requiresRoot: Boolean = false
)

object HardwareFeatureRegistry {
    val spec = HardwareFeatureSpec()
}
