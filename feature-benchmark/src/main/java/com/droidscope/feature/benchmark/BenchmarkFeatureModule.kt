package com.droidscope.feature.benchmark

data class BenchmarkFeatureSpec(
    val id: String = "benchmark",
    val title: String = "Benchmark",
    val requiresRoot: Boolean = false
)

object BenchmarkFeatureRegistry {
    val spec = BenchmarkFeatureSpec()
}
