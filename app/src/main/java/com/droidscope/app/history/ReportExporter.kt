package com.droidscope.app.history

import com.droidscope.core.database.entity.BatterySampleEntity
import java.util.Locale

object ReportExporter {

    fun toCsv(samples: List<BatterySampleEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp_ms,level_percent,temperature_c,voltage_v,current_ua,charge_counter_uah,is_charging")
        samples.forEach { s ->
            sb.append(s.timestampMs).append(',')
            sb.append(s.levelPercent).append(',')
            sb.append(s.temperatureC?.let { String.format(Locale.US, "%.2f", it) } ?: "").append(',')
            sb.append(s.voltageV?.let { String.format(Locale.US, "%.3f", it) } ?: "").append(',')
            sb.append(s.currentMicroAmps ?: "").append(',')
            sb.append(s.chargeCounterMicroAh ?: "").append(',')
            sb.appendLine(s.isCharging)
        }
        return sb.toString()
    }

    fun toJson(samples: List<BatterySampleEntity>): String {
        val items = samples.joinToString(",\n") { s ->
            buildString {
                append("  {")
                append("\"timestampMs\":${s.timestampMs},")
                append("\"levelPercent\":${s.levelPercent},")
                append("\"temperatureC\":${s.temperatureC ?: "null"},")
                append("\"voltageV\":${s.voltageV ?: "null"},")
                append("\"currentMicroAmps\":${s.currentMicroAmps ?: "null"},")
                append("\"chargeCounterMicroAh\":${s.chargeCounterMicroAh ?: "null"},")
                append("\"isCharging\":${s.isCharging}")
                append("}")
            }
        }
        return "[\n$items\n]"
    }
}
