package com.droidscope.app.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.core.database.dao.TelemetryDao
import com.droidscope.core.database.entity.BatterySampleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val telemetryDao: TelemetryDao
) : ViewModel() {

    val batterySamples: StateFlow<List<BatterySampleEntity>> = telemetryDao
        .observeRecentBatterySamples(limit = 100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
