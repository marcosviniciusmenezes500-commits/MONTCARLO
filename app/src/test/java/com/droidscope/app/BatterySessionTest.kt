package com.droidscope.app

import com.droidscope.core.database.entity.BatterySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatterySessionTest {

    @Test
    fun batterySession_tracksDeltaAndFastChargingFlag() {
        val session = BatterySessionEntity(
            id = 1L,
            startTimestampMs = 1_700_000_000_000L,
            endTimestampMs = 1_700_003_600_000L,
            isCharging = true,
            startLevelPercent = 20,
            endLevelPercent = 85,
            startChargeCounterMicroAh = 1_000_000,
            endChargeCounterMicroAh = 4_250_000,
            avgTemperatureC = 36.2,
            avgPowerWatts = 22.5,
            isFastCharging = true
        )
        assertEquals(65, session.endLevelPercent - session.startLevelPercent)
        assertTrue(session.isFastCharging)
    }
}
