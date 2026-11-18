package com.droidscope.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidscope.core.domain.SensorInfo
import java.util.Locale

@Composable
fun LiveSensorCard(sensorInfo: SensorInfo) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var liveValues by remember { mutableStateOf<List<Float>?>(null) }

    if (expanded) {
        DisposableEffect(sensorInfo.name) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val target = sm.getSensorList(Sensor.TYPE_ALL).firstOrNull {
                it.name == sensorInfo.name && it.vendor == sensorInfo.vendor
            }
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    liveValues = event.values.toList()
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (target != null) {
                sm.registerListener(listener, target, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose {
                sm.unregisterListener(listener)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = sensorInfo.name, style = MaterialTheme.typography.titleMedium)
            Text("Fabricante: ${sensorInfo.vendor}", style = MaterialTheme.typography.bodySmall)
            Text("Tipo: ${sensorInfo.type}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Resolução: ${sensorInfo.resolution} | Alcance: ${sensorInfo.maximumRange} | Consumo: ${sensorInfo.power} mA",
                style = MaterialTheme.typography.bodySmall
            )
            if (expanded) {
                val formatted = liveValues?.mapIndexed { i, v ->
                    String.format(Locale.US, "v[%d]=%.3f", i, v)
                }?.joinToString("  ") ?: "Aguardando leitura do sensor..."
                Text(
                    text = "Ao vivo: $formatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(
                    text = "Toque para monitorar em tempo real",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
