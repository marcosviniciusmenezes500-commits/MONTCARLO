package com.droidscope.app.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.core.domain.DeviceSnapshot
import java.util.Locale

@Composable
fun CustomizableDashboardScreen(
    snapshot: DeviceSnapshot,
    battery: BatteryTelemetry,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var editMode by rememberSaveable { mutableStateOf(false) }

    val alerts = buildList {
        battery.temperatureC?.let { temp ->
            if (temp >= prefs.tempAlertThresholdC) {
                add(String.format(Locale.US, "Temperatura da bateria elevada: %.1f °C (limite: %.0f °C)", temp, prefs.tempAlertThresholdC))
            }
        }
        if (snapshot.ramGb > 0 && (snapshot.ramUsedGb / snapshot.ramGb.toDouble()) >= 0.85) {
            add("Uso de RAM acima de 85%")
        }
        if (snapshot.storageGb > 0 && (snapshot.storageAvailableGb / snapshot.storageGb.toDouble()) <= 0.10) {
            add("Armazenamento livre abaixo de 10%")
        }
    }

    val cardsToDisplay = if (editMode) {
        prefs.dashboardCardOrder
    } else {
        prefs.dashboardCardOrder.filterNot { it in prefs.hiddenDashboardCards }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${snapshot.manufacturer} ${snapshot.model}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editMode) {
                        TextButton(onClick = { viewModel.resetLayout() }) {
                            Text("Padrão")
                        }
                    }
                    OutlinedButton(onClick = { editMode = !editMode }) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Text(if (editMode) " Concluir" else " Personalizar")
                    }
                }
            }
        }

        if (alerts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Alerta",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = " Alertas Ativos",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        alerts.forEach { msg ->
                            Text(
                                text = "• $msg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        itemsIndexed(cardsToDisplay, key = { _, id -> id }) { index, cardId ->
            val isHidden = cardId in prefs.hiddenDashboardCards
            DashboardMetricCard(
                cardId = cardId,
                snapshot = snapshot,
                battery = battery,
                editMode = editMode,
                isHidden = isHidden,
                canMoveUp = index > 0,
                canMoveDown = index < cardsToDisplay.lastIndex,
                onMoveUp = { viewModel.moveCard(cardId, -1) },
                onMoveDown = { viewModel.moveCard(cardId, +1) },
                onToggleHidden = { viewModel.toggleVisibility(cardId) }
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    cardId: String,
    snapshot: DeviceSnapshot,
    battery: BatteryTelemetry,
    editMode: Boolean,
    isHidden: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleHidden: () -> Unit
) {
    val title = when (cardId) {
        "DEVICE" -> "Sistema & Dispositivo"
        "BATTERY" -> "Bateria"
        "CPU" -> "Processador (CPU)"
        "MEMORY" -> "Memória RAM"
        "STORAGE" -> "Armazenamento"
        "NETWORK" -> "Rede & Atividade"
        else -> cardId
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHidden) "$title (Oculto)" else title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (editMode) {
                    Row {
                        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Subir")
                        }
                        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Descer")
                        }
                        IconButton(onClick = onToggleHidden) {
                            Icon(
                                if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Alternar visibilidade"
                            )
                        }
                    }
                }
            }

            if (!isHidden) {
                when (cardId) {
                    "DEVICE" -> {
                        Text("${snapshot.androidVersion} | Patch: ${snapshot.securityPatch}")
                        Text("SoC: ${snapshot.socModel} (${snapshot.cpuArchitecture})")
                    }
                    "BATTERY" -> {
                        val pct = (battery.levelPercent.coerceIn(0, 100)) / 100f
                        val temp = battery.temperatureC?.let { String.format(Locale.US, "%.1f °C", it) } ?: "N/D"
                        val power = battery.estimatedPowerWatts?.let { String.format(Locale.US, "%.2f W", it) } ?: "N/D"
                        Text("${battery.levelPercent}% (${battery.statusLabel}) | Temp: $temp | Potência: $power")
                        LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                    }
                    "CPU" -> {
                        val freqs = snapshot.cpuCoresInfo.mapNotNull { it.currentFrequencyMhz }
                        val avgFreq = if (freqs.isNotEmpty()) "${freqs.average().toInt()} MHz (média)" else "Frequência restrita"
                        Text(snapshot.processor)
                        Text("Clock atual: $avgFreq")
                    }
                    "MEMORY" -> {
                        val frac = if (snapshot.ramGb > 0) (snapshot.ramUsedGb / snapshot.ramGb.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
                        Text(String.format(Locale.US, "Em uso: %.2f GB / %d GB (Livre: %.2f GB)", snapshot.ramUsedGb, snapshot.ramGb, snapshot.ramAvailableGb))
                        LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                    }
                    "STORAGE" -> {
                        val frac = if (snapshot.storageGb > 0) (snapshot.storageUsedGb / snapshot.storageGb.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
                        Text(String.format(Locale.US, "Usado: %.2f GB / %d GB (Livre: %.2f GB)", snapshot.storageUsedGb, snapshot.storageGb, snapshot.storageAvailableGb))
                        LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                    }
                    "NETWORK" -> {
                        Text("Conexão: ${snapshot.connectionStatus}")
                        Text("Tempo de atividade: ${snapshot.uptimeHours} h | Sensores: ${snapshot.sensors.size}")
                    }
                }
            }
        }
    }
}
