package com.droidscope.app

import com.droidscope.app.benchmark.DroidScopeBenchmarkEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DroidScopeCoreTest {

    @Test
    fun cpuScore_scalesWithThreadsAndSpeed() {
        val singleFast = DroidScopeBenchmarkEngine.calculateCpuScore(elapsedMs = 100L, threads = 1)
        val singleSlow = DroidScopeBenchmarkEngine.calculateCpuScore(elapsedMs = 400L, threads = 1)
        val multi = DroidScopeBenchmarkEngine.calculateCpuScore(elapsedMs = 200L, threads = 8)

        assertEquals(1200, singleFast)
        assertEquals(300, singleSlow)
        assertTrue(multi > singleFast)
    }

    @Test
    fun cpuScore_handlesZeroElapsedSafely() {
        val score = DroidScopeBenchmarkEngine.calculateCpuScore(elapsedMs = 0L, threads = 1)
        assertTrue(score > 0)
    }
}
