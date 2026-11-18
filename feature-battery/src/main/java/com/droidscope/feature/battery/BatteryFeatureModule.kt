package com.droidscope.feature.battery

data class BatteryFeatureSpec(
    val id: String = "battery",
    val title: String = "Battery",
    val requiresRoot: Boolean = false
)

object BatteryFeatureRegistry {
    val spec = BatteryFeatureSpec()
}
