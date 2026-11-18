package com.droidscope.feature.sensors

data class SensorsFeatureSpec(
    val id: String = "sensors",
    val title: String = "Sensors",
    val requiresRoot: Boolean = false
)

object SensorsFeatureRegistry {
    val spec = SensorsFeatureSpec()
}
