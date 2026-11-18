package com.droidscope.app.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.core.database.dao.TelemetryDao
import com.droidscope.core.database.entity.DiagnosticEventEntity
import com.droidscope.core.domain.DeviceSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val engine: DiagnosticsEngine,
    private val telemetryDao: TelemetryDao
) : ViewModel() {

    private val _lastRunStatus = MutableStateFlow("Execute uma verificação para analisar o dispositivo.")
    val lastRunStatus: StateFlow<String> = _lastRunStatus.asStateFlow()

    val events: StateFlow<List<DiagnosticEventEntity>> = telemetryDao
        .observeDiagnosticEvents(limit = 50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun runDiagnostic(snapshot: DeviceSnapshot, battery: BatteryTelemetry) {
        viewModelScope.launch {
            val findings = engine.analyze(snapshot, battery)
            if (findings.isEmpty()) {
                _lastRunStatus.value = "Nenhuma anomalia detectada nos sensores e recursos verificados."
            } else {
                findings.forEach { telemetryDao.insertDiagnosticEvent(it) }
                _lastRunStatus.value = "${findings.size} alerta(s) registrado(s) com evidências reais."
            }
        }
    }
}
