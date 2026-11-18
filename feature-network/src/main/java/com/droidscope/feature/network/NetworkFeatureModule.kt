package com.droidscope.feature.network

data class NetworkFeatureSpec(
    val id: String = "network",
    val title: String = "Network",
    val requiresRoot: Boolean = false
)

object NetworkFeatureRegistry {
    val spec = NetworkFeatureSpec()
}
