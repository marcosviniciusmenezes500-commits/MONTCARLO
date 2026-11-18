package com.droidscope.app.history

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    battery: BatteryTelemetry,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val samples by viewModel.batterySamples.collectAsStateWithLifecycle()
    val df = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

    // Estimativa transparente baseada em ChargeCounter / percentual (quando exposto pelo fabricante)
    val estimatedFullMah = if (battery.chargeCounterMicroAh != null && battery.levelPercent in 10..100) {
        ((battery.chargeCounterMicroAh / 1000.0) / (battery.levelPercent / 100.0)).roundToInt()
    } else null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Estimativa de Capacidade & Histórico", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (estimatedFullMah != null) {
                            "Capacidade total estimada: ~$estimatedFullMah mAh (Metodologia: ChargeCounter ÷ nível atual; margem de incerteza ±5–10% dependendo da calibração do controlador)."
                        } else {
                            "Estimativa indisponível no momento (requer ChargeCounter exposto pelo controlador e carga ≥ 10%)."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { viewModel.recordBatterySample(battery) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrar Amostra Agora no Room")
                    }
                }
            }
        }

        item {
            Text("Amostras Salvas (${samples.size})", style = MaterialTheme.typography.titleMedium)
        }

        items(samples, key = { it.id }) { s ->
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
