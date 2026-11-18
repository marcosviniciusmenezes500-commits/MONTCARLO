package com.droidscope.feature.cpu

data class CpuFeatureSpec(
    val id: String = "cpu",
    val title: String = "Cpu",
    val requiresRoot: Boolean = false
)

object CpuFeatureRegistry {
    val spec = CpuFeatureSpec()
}
