package com.droidscope.app.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.droidscope.app.data.BatteryMetricsReader
import com.droidscope.app.data.UserPreferencesRepository
import com.droidscope.core.database.dao.TelemetryDao
import com.droidscope.core.database.entity.BatterySampleEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BatteryTelemetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val batteryMetricsReader: BatteryMetricsReader,
    private val telemetryDao: TelemetryDao,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val battery = batteryMetricsReader.read()
            val now = System.currentTimeMillis()
            telemetryDao.insertBatterySample(
                BatterySampleEntity(
                    timestampMs = now,
                    levelPercent = battery.levelPercent,
                    temperatureC = battery.temperatureC,
                    voltageV = battery.voltageV,
                    currentMicroAmps = battery.currentNowMicroAmps,
                    chargeCounterMicroAh = battery.chargeCounterMicroAh,
                    isCharging = battery.isCharging
                )
            )
            // Limpeza automática baseada na retenção configurada
            val prefs = preferencesRepository.preferencesFlow.first()
            val cutoffMs = now - (prefs.retentionDays.toLong() * 24L * 60L * 60L * 1000L)
            telemetryDao.pruneBatterySamples(cutoffMs)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
