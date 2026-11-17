package com.droidscope.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidscope.core.domain.DeviceSnapshot

@Composable
fun DashboardScreen(
    snapshot: DeviceSnapshot,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "DroidScope",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            "Device Intelligence",
            style = MaterialTheme.typography.bodyMedium
        )

        // DISPOSITIVO

        InfoCard("DISPOSITIVO") {
            InfoLine("Fabricante", snapshot.manufacturer)
            InfoLine("Modelo", snapshot.model)
            InfoLine("Sistema", snapshot.androidVersion)
            InfoLine("Processador", snapshot.processor)
            InfoLine("SoC", snapshot.socModel)
            InfoLine("Hardware", snapshot.hardware)
        }

        // PROCESSADOR

        InfoCard("PROCESSADOR") {
            InfoLine("Arquitetura", snapshot.cpuArchitecture)

            InfoLine(
                "Núcleos",
                "${snapshot.cpuCoresInfo.size.takeIf { it > 0 } ?: Runtime.getRuntime().availableProcessors()}"
            )

            Text(
                "Frequências por núcleo",
                style = MaterialTheme.typography.titleSmall
            )

            if (snapshot.cpuCoresInfo.isEmpty()) {
                Text("Frequências não disponibilizadas pelo fabricante")
            } else {
                snapshot.cpuCoresInfo.forEach { core ->

                    InfoLine(
                        "CPU ${core.core}",
                        "Atual: ${
                            core.currentFrequencyMhz?.let { "$it MHz" }
                                ?: "Indisponível"
                        } | Máxima: ${
                            core.maximumFrequencyMhz?.let { "$it MHz" }
                                ?: "Indisponível"
                        }"
                    )
                }
            }

            Text(
                "ABIs suportadas",
                style = MaterialTheme.typography.titleSmall
            )

            snapshot.supportedAbis.forEach {
                Text(it)
            }
        }

        // MEMÓRIA RAM

        InfoCard("MEMÓRIA RAM") {
            InfoLine("Total", "${snapshot.ramGb} GB")
            InfoLine(
                "Utilizada (estimativa)",
                "%.2f GB".format(snapshot.ramUsedGb)
            )
            InfoLine(
                "Disponível",
                "%.2f GB".format(snapshot.ramAvailableGb)
            )

            LinearProgressIndicator(
                progress = {
                    if (snapshot.ramGb > 0)
                        (snapshot.ramUsedGb / snapshot.ramGb)
                            .toFloat().coerceIn(0f, 1f)
                    else 0f
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ARMAZENAMENTO

        InfoCard("ARMAZENAMENTO") {
            InfoLine("Total", "${snapshot.storageGb} GB")
            InfoLine(
                "Utilizado",
                "%.2f GB".format(snapshot.storageUsedGb)
            )
            InfoLine(
                "Disponível",
                "%.2f GB".format(snapshot.storageAvailableGb)
            )

            LinearProgressIndicator(
                progress = {
                    if (snapshot.storageGb > 0)
                        (snapshot.storageUsedGb / snapshot.storageGb)
                            .toFloat().coerceIn(0f, 1f)
                    else 0f
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // TELA

        InfoCard("TELA") {
            InfoLine("Resolução", snapshot.displayResolution)
            InfoLine("Densidade", "${snapshot.densityDpi} DPI")
            InfoLine(
                "Taxa de atualização",
                if (snapshot.refreshRateHz > 0)
                    "%.1f Hz".format(snapshot.refreshRateHz)
                else "Indisponível"
            )
        }

        // BATERIA

        InfoCard("BATERIA") {
            InfoLine(
                "Carga",
                if (snapshot.batteryPercent >= 0)
                    "${snapshot.batteryPercent}%"
                else "Indisponível"
            )

            InfoLine(
                "Temperatura",
                if (snapshot.batteryTempC >= 0)
                    "%.1f °C".format(snapshot.batteryTempC)
                else "Indisponível"
            )

            InfoLine(
                "Tensão",
                if (snapshot.batteryVoltageV >= 0)
                    "%.2f V".format(snapshot.batteryVoltageV)
                else "Indisponível"
            )

            InfoLine(
                "Carregamento",
                if (snapshot.isCharging)
                    "Carregando"
                else "Não carregando"
            )
        }

        // MONITORAMENTO REAL DOS SENSORES
        SensorLivePanel()

        // SENSORES

        InfoCard("SENSORES") {

            InfoLine(
                "Sensores identificados",
                "${snapshot.sensors.size}"
            )

            snapshot.sensors.take(12).forEach { sensor ->

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        sensor.name,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        "Fabricante: ${sensor.vendor}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "Tipo: ${sensor.type}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "Resolução: ${sensor.resolution} | Alcance: ${sensor.maximumRange}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (snapshot.sensors.size > 12) {
                Text(
                    "Mais ${snapshot.sensors.size - 12} sensores identificados"
                )
            }
        }

        // SISTEMA

        InfoCard("SISTEMA OPERACIONAL") {
            InfoLine("Kernel", snapshot.kernelVersion)
            InfoLine("Patch de segurança", snapshot.securityPatch)
            InfoLine("Rede", snapshot.connectionStatus)
            InfoLine("Tempo ligado", "${snapshot.uptimeHours} horas")
        }

        Text(
            "Coleta automática a cada 5 segundos. " +
                "Algumas informações dependem das APIs e permissões " +
                "disponibilizadas pelo fabricante.",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium
            )

            content()
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
