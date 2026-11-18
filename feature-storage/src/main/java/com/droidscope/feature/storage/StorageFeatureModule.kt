package com.droidscope.feature.storage

data class StorageFeatureSpec(
    val id: String = "storage",
    val title: String = "Storage",
    val requiresRoot: Boolean = false
)

object StorageFeatureRegistry {
    val spec = StorageFeatureSpec()
}
