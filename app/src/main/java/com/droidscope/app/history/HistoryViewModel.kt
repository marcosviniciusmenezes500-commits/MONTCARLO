package com.droidscope.app.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.core.database.dao.TelemetryDao
import com.droidscope.core.database.entity.BatterySampleEntity
import com.droidscope.core.database.entity.BatterySessionEntity
import com.droidscope.core.database.entity.SystemSampleEntity
import com.droidscope.core.domain.DeviceSnapshot
import com.droidscope.feature.battery.BatteryHealthEstimate
import com.droidscope.feature.battery.BatteryHealthEstimator
import com.droidscope.feature.battery.BatteryMeasurementPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

enum class HistoryPeriod(val label: String, val durationMs: Long) {
    LAST_24H("24h", 24L * 60L * 60L * 1000L),
    LAST_7D("7 dias", 7L * 24L * 60L * 60L * 1000L),
    LAST_30D("30 dias", 30L * 24L * 60L * 60L * 1000L)
}

data class PeriodComparison(
    val currentCount: Int,
    val previousCount: Int,
    val currentAvgTempC: Double?,
    val previousAvgTempC: Double?,
    val currentAvgDischargeMa: Double?,
    val previousAvgDischargeMa: Double?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val telemetryDao: TelemetryDao
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(HistoryPeriod.LAST_24H)
    val selectedPeriod: StateFlow<HistoryPeriod> = _selectedPeriod.asStateFlow()

    private val _designCapacityMah = MutableStateFlow(5000)
    val designCapacityMah: StateFlow<Int> = _designCapacityMah.asStateFlow()

    val batterySamples: StateFlow<List<BatterySampleEntity>> = _selectedPeriod
        .flatMapLatest { period ->
            telemetryDao.observeBatterySamplesSince(System.currentTimeMillis() - period.durationMs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batterySessions: StateFlow<List<BatterySessionEntity>> = telemetryDao
        .observeBatterySessions(limit = 30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemSamples: StateFlow<List<SystemSampleEntity>> = _selectedPeriod
        .flatMapLatest { period ->
            telemetryDao.observeSystemSamplesSince(System.currentTimeMillis() - period.durationMs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthEstimate: StateFlow<BatteryHealthEstimate> = combine(
        batterySamples,
        _designCapacityMah
    ) { samples, designMah ->
        val points = samples.map {
            BatteryMeasurementPoint(
                timestampMs = it.timestampMs,
                levelPercent = it.levelPercent,
                currentMicroAmps = it.currentMicroAmps,
                chargeCounterMicroAh = it.chargeCounterMicroAh,
                isCharging = it.isCharging
            )
        }
        BatteryHealthEstimator.estimate(points, designCapacityMah = designMah)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BatteryHealthEstimator.estimate(emptyList(), 5000)
    )

    val comparison: StateFlow<PeriodComparison> = _selectedPeriod
        .flatMapLatest { period ->
            val now = System.currentTimeMillis()
            val currentStart = now - period.durationMs
            val prevStart = currentStart - period.durationMs
            combine(
                telemetryDao.observeBatterySamplesSince(currentStart),
                telemetryDao.observeBatterySamplesBetween(prevStart, currentStart)
            ) { current, previous ->
                PeriodComparison(
                    currentCount = current.size,
                    previousCount = previous.size,
                    currentAvgTempC = current.mapNotNull { it.temperatureC }.takeIf { it.isNotEmpty() }?.average(),
                    previousAvgTempC = previous.mapNotNull { it.temperatureC }.takeIf { it.isNotEmpty() }?.average(),
                    currentAvgDischargeMa = current.filter { !it.isCharging }
                        .mapNotNull { it.currentMicroAmps?.let { ua -> abs(ua) / 1000.0 } }
                        .takeIf { it.isNotEmpty() }?.average(),
                    previousAvgDischargeMa = previous.filter { !it.isCharging }
                        .mapNotNull { it.currentMicroAmps?.let { ua -> abs(ua) / 1000.0 } }
                        .takeIf { it.isNotEmpty() }?.average()
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PeriodComparison(0, 0, null, null, null, null)
        )

    fun selectPeriod(period: HistoryPeriod) {
        _selectedPeriod.value = period
    }

    fun setDesignCapacityMah(mah: Int) {
        _designCapacityMah.value = mah.coerceIn(1000, 15000)
    }

    fun recordSnapshotSample(snapshot: DeviceSnapshot, battery: BatteryTelemetry) {
        viewModelScope.launch {
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
        }
    }
}
