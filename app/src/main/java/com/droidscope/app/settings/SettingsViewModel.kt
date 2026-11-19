package com.droidscope.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidscope.app.background.MonitoringScheduler
import com.droidscope.app.data.UserPreferences
import com.droidscope.app.data.UserPreferencesRepository
import com.droidscope.core.database.dao.TelemetryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val telemetryDao: TelemetryDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val monitoringScheduler: MonitoringScheduler
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkTheme(enabled) }
    }

    fun setSamplingIntervalMs(intervalMs: Long) {
        viewModelScope.launch { preferencesRepository.setSamplingIntervalMs(intervalMs) }
    }

    fun setTempAlertThresholdC(thresholdC: Double) {
        viewModelScope.launch { preferencesRepository.setTempAlertThresholdC(thresholdC) }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch { preferencesRepository.setRetentionDays(days) }
    }

    fun setBackgroundMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundMonitoring(enabled)
            monitoringScheduler.setEnabled(enabled)
            _message.value = if (enabled) {
                "Coleta periódica em segundo plano ativada (a cada 15 min via WorkManager)."
            } else {
                "Coleta periódica em segundo plano desativada."
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            telemetryDao.clearBatteryHistory()
            telemetryDao.clearSystemHistory()
            telemetryDao.clearSystemHistory()
            telemetryDao.clearBenchmarkHistory()
            telemetryDao.clearDiagnosticHistory()
            _message.value = "Todos os registros locais do Room foram apagados."
        }
    }
}
