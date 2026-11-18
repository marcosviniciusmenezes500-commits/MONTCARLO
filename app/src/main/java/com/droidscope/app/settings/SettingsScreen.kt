package com.droidscope.app.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.core.domain.DeviceSnapshot
import java.util.Locale

@Composable
fun SettingsScreen(
    snapshot: DeviceSnapshot,
    battery: BatteryTelemetry,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tema Escuro", style = MaterialTheme.typography.titleMedium)
                    Text("Visual profissional de diagnóstico", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = isDarkTheme, onCheckedChange = onToggleTheme)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Relatório & Privacidade (100% Offline)", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = {
                        val report = buildString {
                            appendLine("=== RELATÓRIO DROIDSCOPE ===")
                            appendLine("Dispositivo: ${snapshot.manufacturer} ${snapshot.model}")
                            appendLine("Sistema: ${snapshot.androidVersion} | Patch: ${snapshot.securityPatch}")
                            appendLine("SoC: ${snapshot.socModel} | Hardware: ${snapshot.hardware}")
                            appendLine("CPU: ${snapshot.processor} (${snapshot.cpuArchitecture})")
                            appendLine("RAM: ${String.format(Locale.US, "%.2f / %d GB", snapshot.ramUsedGb, snapshot.ramGb)}")
                            appendLine("Armazenamento: ${String.format(Locale.US, "%.2f / %d GB", snapshot.storageUsedGb, snapshot.storageGb)}")
                            appendLine("Tela: ${snapshot.displayResolution} @ ${String.format(Locale.US, "%.1f Hz", snapshot.refreshRateHz)}")
                            appendLine("Bateria: ${battery.levelPercent}% (${battery.statusLabel}) | Saúde: ${battery.healthLabel}")
                            appendLine("Temp Bateria: ${battery.temperatureC ?: "N/D"} °C | Tensão: ${battery.voltageV ?: "N/D"} V")
                            appendLine("Sensores detectados: ${snapshot.sensors.size}")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Relatório DroidScope - ${snapshot.model}")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Exportar relatório DroidScope"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exportar / Compartilhar Relatório")
                }

                OutlinedButton(
                    onClick = { viewModel.clearAllHistory() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Limpar Histórico Local (Room)")
                }

                message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
