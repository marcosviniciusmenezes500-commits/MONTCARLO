package com.droidscope.app.history

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.app.ui.components.RealtimeLineChartCard
import com.droidscope.core.domain.DeviceSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    snapshot: DeviceSnapshot,
    battery: BatteryTelemetry,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val samples by viewModel.batterySamples.collectAsStateWithLifecycle()
    val systemSamples by viewModel.systemSamples.collectAsStateWithLifecycle()
    val health by viewModel.healthEstimate.collectAsStateWithLifecycle()
    val designMah by viewModel.designCapacityMah.collectAsStateWithLifecycle()
    val comparison by viewModel.comparison.collectAsStateWithLifecycle()
    val df = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

    val tempHistory = samples.mapNotNull { it.temperatureC?.toFloat() }
    val ramHistory = systemSamples.map { it.ramUsedPercent.toFloat() }
    val cpuHistory = systemSamples.mapNotNull { it.avgCpuFreqMhz?.toFloat() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) },
                        label = { Text(period.label) }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Saúde da Bateria (Integrada no Tempo)", style = MaterialTheme.typography.titleMedium)
                    Text("Capacidade nominal de referência (mAh):", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(4000, 4500, 5000, 5500, 6000).forEach { cap ->
                            FilterChip(
                                selected = designMah == cap,
                                onClick = { viewModel.setDesignCapacityMah(cap) },
                                label = { Text("$cap") }
                            )
                        }
                    }
                    Text(
                        text = health.estimatedCapacityMah?.let { est ->
                            val hPct = health.healthPercent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/D"
                            val deg = health.degradationPercent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/D"
                            "Estimada: $est mAh / $designMah mAh | Saúde: $hPct | Degradação: $deg"
                        } ?: "Estimada: aguardando variação de carga acumulada.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Confiança: ${health.confidence.label} (${health.sampleCount} amostras, Δ${health.totalDeltaPercentObserved}%)", style = MaterialTheme.typography.bodySmall)
                    Text("Metodologia: ${health.methodology}", style = MaterialTheme.typography.bodySmall)

                    val curTemp = comparison.currentAvgTempC?.let { String.format(Locale.US, "%.1f °C", it) } ?: "N/D"
                    val prevTemp = comparison.previousAvgTempC?.let { String.format(Locale.US, "%.1f °C", it) } ?: "N/D"
                    val curDis = comparison.currentAvgDischargeMa?.let { String.format(Locale.US, "%.0f mA", it) } ?: "N/D"
                    val prevDis = comparison.previousAvgDischargeMa?.let { String.format(Locale.US, "%.0f mA", it) } ?: "N/D"

                    Text("Temp. média: $curTemp (anterior: $prevTemp)", style = MaterialTheme.typography.bodySmall)
                    Text("Descarga média: $curDis (anterior: $prevDis)", style = MaterialTheme.typography.bodySmall)

                    Button(
                        onClick = { viewModel.recordSnapshotSample(snapshot, battery) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrar Amostra (Bateria + CPU/RAM)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csv = ReportExporter.toCsv(samples)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "DroidScope Histórico (${selectedPeriod.label}).csv")
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                }
                                context.startActivity(Intent.createChooser(intent, "Exportar CSV"))
                            },
                            enabled = samples.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Exportar CSV")
                        }
                        OutlinedButton(
                            onClick = {
                                val json = ReportExporter.toJson(samples)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, "DroidScope Histórico (${selectedPeriod.label}).json")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                context.startActivity(Intent.createChooser(intent, "Exportar JSON"))
                            },
                            enabled = samples.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Exportar JSON")
                        }
                    }
                }
            }
        }

        if (tempHistory.size >= 2) {
            item {
                RealtimeLineChartCard(
                    title = "Histórico de Temperatura (${selectedPeriod.label})",
                    unit = "°C",
                    values = tempHistory,
                    lineColor = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (ramHistory.size >= 2) {
            item {
                RealtimeLineChartCard(
                    title = "Histórico de Uso de RAM (${selectedPeriod.label})",
                    unit = "%",
                    values = ramHistory,
                    lineColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (cpuHistory.size >= 2) {
            item {
                RealtimeLineChartCard(
                    title = "Histórico de Clock Médio CPU (${selectedPeriod.label})",
                    unit = "MHz",
                    values = cpuHistory,
                    lineColor = MaterialTheme.colorScheme.secondary
                )
            }
        }

        item {
            Text("Registros de Bateria (${samples.size}) | Sistema (${systemSamples.size})", style = MaterialTheme.typography.titleMedium)
        }

        items(samples.reversed(), key = { it.id }) { s ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val temp = s.temperatureC?.let { String.format(Locale.US, "%.1f°C", it) } ?: "N/D"
                    val volt = s.voltageV?.let { String.format(Locale.US, "%.2fV", it) } ?: "N/D"
                    val curr = s.currentMicroAmps?.let { "${it / 1000} mA" } ?: "N/D"
                    Text(
                        text = "${df.format(Date(s.timestampMs))} — ${s.levelPercent}% (${if (s.isCharging) "Carregando" else "Descarga"})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("Temp: $temp | Tensão: $volt | Corrente: $curr", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
