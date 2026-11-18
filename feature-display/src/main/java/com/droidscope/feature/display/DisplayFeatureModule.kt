package com.droidscope.feature.display

data class DisplayFeatureSpec(
    val id: String = "display",
    val title: String = "Display",
    val requiresRoot: Boolean = false
)

object DisplayFeatureRegistry {
    val spec = DisplayFeatureSpec()
}
