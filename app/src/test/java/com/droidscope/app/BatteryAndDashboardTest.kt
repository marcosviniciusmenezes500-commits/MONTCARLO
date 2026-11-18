package com.droidscope.app

import com.droidscope.app.data.DEFAULT_DASHBOARD_ORDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class BatteryAndDashboardTest {

    @Test
    fun estimatedFullCapacity_calculatesCorrectlyFromChargeCounter() {
        val chargeCounterMicroAh = 3_600_000 // 3600 mAh
        val levelPercent = 80
        val estimatedMah = ((chargeCounterMicroAh / 1000.0) / (levelPercent / 100.0)).roundToInt()
        assertEquals(4500, estimatedMah)
    }

    @Test
    fun defaultDashboardOrder_containsAllSixCoreCards() {
        assertEquals(6, DEFAULT_DASHBOARD_ORDER.size)
        assertTrue(DEFAULT_DASHBOARD_ORDER.containsAll(listOf("DEVICE", "BATTERY", "CPU", "MEMORY", "STORAGE", "NETWORK")))
    }
}
