package com.droidscope.app.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.core.database.dao.TelemetryDao
import com.droidscope.core.database.entity.BatterySampleEntity
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

    val batterySamples: StateFlow<List<BatterySampleEntity>> = _selectedPeriod
        .flatMapLatest { period ->
            val since = System.currentTimeMillis() - period.durationMs
            telemetryDao.observeBatterySamplesSince(since)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun recordBatterySample(battery: BatteryTelemetry) {
        viewModelScope.launch {
            telemetryDao.insertBatterySample(
                BatterySampleEntity(
                    timestampMs = System.currentTimeMillis(),
                    levelPercent = battery.levelPercent,
                    temperatureC = battery.temperatureC,
                    voltageV = battery.voltageV,
                    currentMicroAmps = battery.currentNowMicroAmps,
                    chargeCounterMicroAh = battery.chargeCounterMicroAh,
                    isCharging = battery.isCharging
                )
            )
        }
    }
}
