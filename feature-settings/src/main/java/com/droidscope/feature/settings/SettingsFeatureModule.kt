package com.droidscope.feature.settings

data class SettingsFeatureSpec(
    val id: String = "settings",
    val title: String = "Settings",
    val requiresRoot: Boolean = false
)

object SettingsFeatureRegistry {
    val spec = SettingsFeatureSpec()
}
