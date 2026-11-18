package com.droidscope.app

import com.droidscope.app.history.ReportExporter
import com.droidscope.core.database.entity.BatterySampleEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExporterTest {

    @Test
    fun toCsv_containsHeaderAndFormattedRow() {
        val samples = listOf(
            BatterySampleEntity(
                id = 1,
                timestampMs = 1700000000000L,
                levelPercent = 82,
                temperatureC = 34.5,
                voltageV = 4.125,
                currentMicroAmps = -450000,
                chargeCounterMicroAh = 3900000,
                isCharging = false
            )
        )
        val csv = ReportExporter.toCsv(samples)
        assertTrue(csv.startsWith("timestamp_ms,level_percent,temperature_c,voltage_v,current_ua,charge_counter_uah,is_charging"))
        assertTrue(csv.contains("1700000000000,82,34.50,4.125,-450000,3900000,false"))
    }

    @Test
    fun toJson_formatsArrayWithNullSafety() {
        val samples = listOf(
            BatterySampleEntity(
                id = 2,
                timestampMs = 1700000001000L,
                levelPercent = 50,
                temperatureC = null,
                voltageV = 3.9,
                currentMicroAmps = null,
                chargeCounterMicroAh = null,
                isCharging = true
            )
        )
        val json = ReportExporter.toJson(samples)
        assertTrue(json.trim().startsWith("["))
        assertTrue(json.trim().endsWith("]"))
        assertTrue(json.contains("\"levelPercent\":50"))
        assertTrue(json.contains("\"temperatureC\":null"))
        assertTrue(json.contains("\"isCharging\":true"))
    }
}
