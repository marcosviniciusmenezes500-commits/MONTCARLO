package com.droidscope.app.benchmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BenchmarkScreen(
    viewModel: BenchmarkViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DroidScope Benchmark Nativo",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Escala própria DroidScope (não equivalente ao Geekbench). Executa CPU Single/Multi-Core, banda de RAM e I/O sequencial em arquivo temporário.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(text = state.stage, style = MaterialTheme.typography.bodyMedium)
                    if (state.isRunning || state.progress > 0f) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    state.errorMessage?.let { err ->
                        Text(
                            text = "Erro: $err",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { viewModel.startBenchmark() },
                        enabled = !state.isRunning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isRunning) "Executando..." else "Executar Suite Completa")
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Text(
                    text = "Histórico de Execuções (${history.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(history, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${item.suiteName} — ${dateFormat.format(Date(item.timestampMs))}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("CPU Single-Core: ${item.singleCoreScore ?: "N/D"} pts")
                        Text("CPU Multi-Core: ${item.multiCoreScore ?: "N/D"} pts")
                        Text("Memória RAM: ${item.memoryScore ?: "N/D"} pts")
                        Text("Armazenamento: ${item.storageScore ?: "N/D"} pts")
                        val tStart = item.startTempC?.let { "%.1f°C".format(Locale.US, it) } ?: "N/D"
                        val tEnd = item.endTempC?.let { "%.1f°C".format(Locale.US, it) } ?: "N/D"
                        Text("Temperatura: $tStart → $tEnd | Bateria: ${item.batteryPercent}%")
                    }
                }
            }
        }
    }
}
