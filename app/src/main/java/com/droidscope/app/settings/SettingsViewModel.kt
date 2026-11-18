package com.droidscope.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidscope.core.database.dao.TelemetryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val telemetryDao: TelemetryDao
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearAllHistory() {
        viewModelScope.launch {
            telemetryDao.clearBatteryHistory()
            telemetryDao.clearBenchmarkHistory()
            telemetryDao.clearDiagnosticHistory()
            _message.value = "Todos os registros locais do Room foram apagados."
        }
    }
}
