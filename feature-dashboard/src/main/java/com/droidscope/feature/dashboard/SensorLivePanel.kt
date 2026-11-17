package com.droidscope.feature.dashboard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.scan

private data class LiveSensorReading(
    val type: Int,
    val name: String,
    val vendor: String,
    val values: List<Float>,
    val timestamp: Long
)

private val monitoredSensorTypes = listOf(
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_MAGNETIC_FIELD,
    Sensor.TYPE_PROXIMITY,
    Sensor.TYPE_LIGHT,
    Sensor.TYPE_PRESSURE,
    Sensor.TYPE_GRAVITY,
    Sensor.TYPE_LINEAR_ACCELERATION,
    Sensor.TYPE_ROTATION_VECTOR
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun observeDeviceSensors(
    context: Context
): Flow<List<LiveSensorReading>> = callbackFlow {

    val manager = context.getSystemService(
        Context.SENSOR_SERVICE
    ) as SensorManager

    val availableSensors = monitoredSensorTypes
        .mapNotNull { manager.getDefaultSensor(it) }
        .distinctBy { it.type }

    val listener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            val reading = LiveSensorReading(
                type = event.sensor.type,
                name = event.sensor.name,
                vendor = event.sensor.vendor,
                values = event.values.copyOf().toList(),
                timestamp = event.timestamp
            )

            trySend(reading)
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int
        ) {
            // A precisão é fornecida pelo próprio Android.
        }
    }

    availableSensors.forEach { sensor ->
        manager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    awaitClose {
        manager.unregisterListener(listener)
    }
}.scan(emptyList<LiveSensorReading>()) { current, reading ->
    (current.filterNot { it.type == reading.type } + reading)
        .sortedBy { it.type }
}

private fun sensorLabel(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER -> "Acelerômetro"
    Sensor.TYPE_GYROSCOPE -> "Giroscópio"
    Sensor.TYPE_MAGNETIC_FIELD -> "Magnetômetro"
    Sensor.TYPE_PROXIMITY -> "Proximidade"
    Sensor.TYPE_LIGHT -> "Luminosidade"
    Sensor.TYPE_PRESSURE -> "Pressão atmosférica"
    Sensor.TYPE_GRAVITY -> "Gravidade"
    Sensor.TYPE_LINEAR_ACCELERATION -> "Aceleração linear"
    Sensor.TYPE_ROTATION_VECTOR -> "Vetor de rotação"
    else -> "Sensor"
}

private fun sensorUnit(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_GRAVITY,
    Sensor.TYPE_LINEAR_ACCELERATION -> "m/s²"

    Sensor.TYPE_GYROSCOPE -> "rad/s"

    Sensor.TYPE_MAGNETIC_FIELD -> "µT"

    Sensor.TYPE_PROXIMITY -> "cm"

    Sensor.TYPE_LIGHT -> "lux"

    Sensor.TYPE_PRESSURE -> "hPa"

    Sensor.TYPE_ROTATION_VECTOR -> "adimensional"

    else -> ""
}

@Composable
fun SensorLivePanel() {

    val context = LocalContext.current

    val sensorFlow = remember(context) {
        observeDeviceSensors(context.applicationContext)
    }

    val readings by sensorFlow.collectAsState(
        initial = emptyList()
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                "MONITORAMENTO EM TEMPO REAL",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Leituras fornecidas diretamente pelos sensores Android.",
                style = MaterialTheme.typography.bodySmall
            )

            if (readings.isEmpty()) {
                Text(
                    "Aguardando leituras dos sensores disponíveis...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            readings.forEach { reading ->

                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    Text(
                        sensorLabel(reading.type),
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        reading.name,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "Fabricante: ${reading.vendor}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    val labels = listOf("X", "Y", "Z", "W")

                    reading.values.forEachIndexed { index, value ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                labels.getOrElse(index) { "Valor ${index + 1}" },
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                "%.3f %s".format(
                                    value,
                                    sensorUnit(reading.type)
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Text(
                "Os valores dependem da disponibilidade e das características " +
                    "dos sensores do aparelho.",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
