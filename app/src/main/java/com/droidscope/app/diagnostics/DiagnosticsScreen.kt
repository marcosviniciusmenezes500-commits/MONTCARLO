package com.droidscope.app.diagnostics

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.core.domain.DeviceSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    snapshot: DeviceSnapshot,
    battery: BatteryTelemetry,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val status by viewModel.lastRunStatus.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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
                    Text("Central de Diagnóstico", style = MaterialTheme.typography.titleMedium)
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { viewModel.runDiagnostic(snapshot, battery) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Executar Verificação Agora")
                    }
                }
            }
        }

        if (events.isNotEmpty()) {
            item {
                Text("Registros de Diagnóstico (${events.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(events, key = { it.id }) { event ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("[${event.severity}] ${event.title}", style = MaterialTheme.typography.titleSmall)
                        Text(event.description, style = MaterialTheme.typography.bodySmall)
                        Text("Evidência: ${event.evidence}", style = MaterialTheme.typography.bodySmall)
                        Text("Recomendação: ${event.recommendation}", style = MaterialTheme.typography.bodySmall)
                        Text("Data: ${df.format(Date(event.timestampMs))}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
