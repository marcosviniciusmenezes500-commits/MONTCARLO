package com.droidscope.app.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidscope.core.database.dao.TelemetryDao
import com.droidscope.core.database.entity.BenchmarkResultEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BenchmarkUiState(
    val isRunning: Boolean = false,
    val stage: String = "Pronto para iniciar",
    val progress: Float = 0f,
    val errorMessage: String? = null
)

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val engine: DroidScopeBenchmarkEngine,
    private val telemetryDao: TelemetryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<BenchmarkResultEntity>> = telemetryDao
        .observeBenchmarkResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startBenchmark() {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            _uiState.value = BenchmarkUiState(
                isRunning = true,
                stage = "Inicializando...",
                progress = 0.05f
            )
            try {
                val result = engine.runFullSuite { p ->
                    _uiState.value = _uiState.value.copy(
                        stage = p.stage,
                        progress = p.fraction
                    )
                }
                telemetryDao.insertBenchmarkResult(result)
                _uiState.value = BenchmarkUiState(
                    isRunning = false,
                    stage = "Benchmark concluído e salvo no histórico",
                    progress = 1f
                )
            } catch (e: Exception) {
                _uiState.value = BenchmarkUiState(
                    isRunning = false,
                    stage = "Falha durante o benchmark",
                    progress = 0f,
                    errorMessage = e.message ?: "Erro desconhecido"
                )
            }
        }
    }
}
