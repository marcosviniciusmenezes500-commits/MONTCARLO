package com.droidscope.feature.memory

data class MemoryFeatureSpec(
    val id: String = "memory",
    val title: String = "Memory",
    val requiresRoot: Boolean = false
)

object MemoryFeatureRegistry {
    val spec = MemoryFeatureSpec()
}
