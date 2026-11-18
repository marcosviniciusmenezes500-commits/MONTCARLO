package com.droidscope.feature.diagnostics

data class DiagnosticsFeatureSpec(
    val id: String = "diagnostics",
    val title: String = "Diagnostics",
    val requiresRoot: Boolean = false
)

object DiagnosticsFeatureRegistry {
    val spec = DiagnosticsFeatureSpec()
}
