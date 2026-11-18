package com.droidscope.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class BatteryTelemetry(
    val levelPercent: Int,
    val isCharging: Boolean,
    val statusLabel: String,
    val healthLabel: String,
    val technology: String,
    val temperatureC: Double?,
    val voltageV: Double?,
    val currentNowMicroAmps: Int?,
    val chargeCounterMicroAh: Int?,
    val estimatedPowerWatts: Double?,
    val cycleCount: Int?
)

@Singleton
class BatteryMetricsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun read(): BatteryTelemetry {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else -1

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val statusLabel = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
            BatteryManager.BATTERY_STATUS_FULL -> "Completa"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Não carregando"
            else -> "Desconhecido"
        }

        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Boa"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Superaquecida"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Degradada"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobretensão"
            BatteryManager.BATTERY_HEALTH_COLD -> "Fria"
            else -> "Desconhecida"
        }

        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            ?.takeIf { it.isNotBlank() } ?: "Indisponível"

        val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempC = if (tempRaw >= 0) tempRaw / 10.0 else null

        val voltageRaw = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val voltageV = if (voltageRaw > 0) voltageRaw / 1000.0 else null

        val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeIf { it != Int.MIN_VALUE && it != 0 }

        val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeIf { it != Int.MIN_VALUE && it > 0 }

        val powerWatts = if (voltageV != null && currentNow != null) {
            val amps = abs(currentNow) / 1_000_000.0
            voltageV * amps
        } else null

        val cycles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it >= 0 }
        } else null

        return BatteryTelemetry(
            levelPercent = percent,
            isCharging = isCharging,
            statusLabel = statusLabel,
            healthLabel = healthLabel,
            technology = tech,
            temperatureC = tempC,
            voltageV = voltageV,
            currentNowMicroAmps = currentNow,
            chargeCounterMicroAh = chargeCounter,
            estimatedPowerWatts = powerWatts,
            cycleCount = cycles
        )
    }
}
