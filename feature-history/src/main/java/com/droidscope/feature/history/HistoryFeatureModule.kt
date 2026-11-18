package com.droidscope.feature.history

data class HistoryFeatureSpec(
    val id: String = "history",
    val title: String = "History",
    val requiresRoot: Boolean = false
)

object HistoryFeatureRegistry {
    val spec = HistoryFeatureSpec()
}
