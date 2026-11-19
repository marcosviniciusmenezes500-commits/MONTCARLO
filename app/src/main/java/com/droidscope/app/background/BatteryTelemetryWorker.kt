package com.droidscope.app.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.droidscope.app.data.BatteryMetricsReader
import com.droidscope.app.data.UserPreferencesRepository
import com.droidscope.app.readDeviceSnapshot
import com.droidscope.core.database.dao.TelemetryDao
import com.droidscope.core.database.entity.BatterySampleEntity
import com.droidscope.core.database.entity.DiagnosticEventEntity
import com.droidscope.core.database.entity.SystemSampleEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BatteryTelemetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val batteryMetricsReader: BatteryMetricsReader,
    private val telemetryDao: TelemetryDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val alertNotificationHelper: AlertNotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val battery = batteryMetricsReader.read()
            val snapshot = readDeviceSnapshot(applicationContext)

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

            val ramPct = if (snapshot.ramGb > 0) (snapshot.ramUsedGb / snapshot.ramGb.toDouble()) * 100.0 else 0.0
            val storageFreePct = if (snapshot.storageGb > 0) (snapshot.storageAvailableGb / snapshot.storageGb.toDouble()) * 100.0 else 0.0
            val avgCpuMhz = snapshot.cpuCoresInfo.mapNotNull { it.currentFrequencyMhz }.takeIf { it.isNotEmpty() }?.average()?.toInt()

            telemetryDao.insertSystemSample(
                SystemSampleEntity(
                    timestampMs = now,
                    ramUsedPercent = ramPct,
                    storageFreePercent = storageFreePct,
                    avgCpuFreqMhz = avgCpuMhz
                )
            )

            val prefs = preferencesRepository.preferencesFlow.first()
            val cutoffMs = now - (prefs.retentionDays.toLong() * 24L * 60L * 60L * 1000L)
            telemetryDao.pruneBatterySamples(cutoffMs)
            telemetryDao.pruneSystemSamples(cutoffMs)
            val temp = battery.temperatureC
            if (temp != null && temp >= prefs.tempAlertThresholdC) {
                alertNotificationHelper.notifyHighTemperature(temp, prefs.tempAlertThresholdC)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Falha na coleta periódica de telemetria (tentativa ${runAttemptCount + 1})", e)
            runCatching {
                telemetryDao.insertDiagnosticEvent(
                    DiagnosticEventEntity(
                        timestampMs = System.currentTimeMillis(),
                        title = "Falha na coleta em segundo plano",
                        description = "O Worker de telemetria encontrou uma exceção durante a leitura.",
                        severity = "BAIXA",
                        evidence = "${e::class.java.simpleName}: ${e.message ?: "sem mensagem"}",
                        recommendation = "Verifique restrições de economia de bateria do fabricante para o DroidScope."
                    )
                )
            }
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "BatteryTelemetryWorker"
    }
}
