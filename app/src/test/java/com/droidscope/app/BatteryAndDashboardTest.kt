package com.droidscope.app

import com.droidscope.app.data.DEFAULT_DASHBOARD_ORDER
import com.droidscope.feature.battery.BatteryHealthEstimator
import com.droidscope.feature.battery.BatteryMeasurementPoint
import com.droidscope.feature.battery.ConfidenceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAndDashboardTest {

    @Test
    fun batteryHealthEstimator_computesCapacityAndHealthFromDeltaChargeCounter() {
        val t0 = 1_700_000_000_000L
        val points = listOf(
            BatteryMeasurementPoint(t0, 90, -500_000, 4_050_000, false),
            BatteryMeasurementPoint(t0 + 1_800_000L, 70, -500_000, 3_150_000, false),
            BatteryMeasurementPoint(t0 + 3_600_000L, 50, -500_000, 2_250_000, false)
        )
        // Δ40% = 1800 mAh => 100% = 4500 mAh; design = 5000 mAh => 90% saúde, 10% degradação
        val result = BatteryHealthEstimator.estimate(points, designCapacityMah = 5000)

        assertEquals(4500, result.estimatedCapacityMah)
        assertNotNull(result.healthPercent)
        assertEquals(90.0, result.healthPercent!!, 0.1)
        assertEquals(10.0, result.degradationPercent!!, 0.1)
        assertEquals(40, result.totalDeltaPercentObserved)
    }

    @Test
    fun batteryHealthEstimator_returnsInsufficientWhenNotEnoughDelta() {
        val t0 = 1_700_000_000_000L
        val points = listOf(
            BatteryMeasurementPoint(t0, 80, -300_000, 3_600_000, false),
            BatteryMeasurementPoint(t0 + 60_000L, 80, -300_000, 3_598_000, false)
        )
        val result = BatteryHealthEstimator.estimate(points, designCapacityMah = 5000)
        assertNull(result.estimatedCapacityMah)
        assertEquals(ConfidenceLevel.INSUFFICIENT, result.confidence)
    }

    @Test
    fun defaultDashboardOrder_containsAllSixCoreCards() {
        assertEquals(6, DEFAULT_DASHBOARD_ORDER.size)
        assertTrue(DEFAULT_DASHBOARD_ORDER.containsAll(listOf("DEVICE", "BATTERY", "CPU", "MEMORY", "STORAGE", "NETWORK")))
    }
}
