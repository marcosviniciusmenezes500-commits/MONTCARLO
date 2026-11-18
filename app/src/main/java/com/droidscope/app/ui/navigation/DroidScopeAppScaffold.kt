package com.droidscope.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.droidscope.app.benchmark.BenchmarkScreen
import com.droidscope.app.data.AdvancedHardwareSnapshot
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.app.diagnostics.DiagnosticsScreen
import com.droidscope.app.history.HistoryScreen
import com.droidscope.app.settings.SettingsScreen
import com.droidscope.app.ui.components.LiveSensorCard
import com.droidscope.app.ui.components.RealtimeLineChartCard
import com.droidscope.core.domain.DeviceSnapshot
import com.droidscope.app.dashboard.CustomizableDashboardScreen
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidScopeAppScaffold(
    snapshot: DeviceSnapshot,
    batteryTelemetry: BatteryTelemetry,
    advancedHardware: AdvancedHardwareSnapshot,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: DroidScopeDestination.DASHBOARD.route
    val currentDestination = DroidScopeDestination.entries.firstOrNull { it.route == currentRoute }
        ?: DroidScopeDestination.DASHBOARD

    val tempSeries = remember { mutableStateListOf<Float>() }
    val powerSeries = remember { mutableStateListOf<Float>() }
    val ramSeries = remember { mutableStateListOf<Float>() }
    val cpuFreqSeries = remember { mutableStateListOf<Float>() }

    LaunchedEffect(snapshot, batteryTelemetry) {
        batteryTelemetry.temperatureC?.let {
            tempSeries.add(it.toFloat())
            if (tempSeries.size > 30) tempSeries.removeAt(0)
        }
        batteryTelemetry.estimatedPowerWatts?.let {
            powerSeries.add(it.toFloat())
            if (powerSeries.size > 30) powerSeries.removeAt(0)
        }
        if (snapshot.ramGb > 0) {
            ramSeries.add(((snapshot.ramUsedGb / snapshot.ramGb.toDouble()) * 100.0).toFloat())
            if (ramSeries.size > 30) ramSeries.removeAt(0)
        }
        val freqs = snapshot.cpuCoresInfo.mapNotNull { it.currentFrequencyMhz }
        if (freqs.isNotEmpty()) {
            cpuFreqSeries.add(freqs.average().toFloat())
            if (cpuFreqSeries.size > 30) cpuFreqSeries.removeAt(0)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "DroidScope",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                DroidScopeDestination.entries.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label) },
                        selected = dest.route == currentRoute,
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (dest.route != currentRoute) {
                                navController.navigate(dest.route) {
                                    popUpTo(DroidScopeDestination.DASHBOARD.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentDestination.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = DroidScopeDestination.DASHBOARD.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(DroidScopeDestination.DASHBOARD.route) {
                    CustomizableDashboardScreen(snapshot = snapshot, battery = batteryTelemetry)
                }
                composable(DroidScopeDestination.BATTERY.route) {
                    BatteryOverviewContent(batteryTelemetry, tempSeries.toList(), powerSeries.toList())
                }
                composable(DroidScopeDestination.BENCHMARK.route) {
                    BenchmarkScreen()
                }
                composable(DroidScopeDestination.HARDWARE.route) {
                    HardwareOverviewContent(snapshot, advancedHardware)
                }
                composable(DroidScopeDestination.MONITORING.route) {
                    MonitoringOverviewContent(snapshot, advancedHardware, ramSeries.toList(), cpuFreqSeries.toList())
                }
                composable(DroidScopeDestination.SENSORS.route) {
                    SensorsOverviewContent(snapshot)
                }
                composable(DroidScopeDestination.DIAGNOSTICS.route) {
                    DiagnosticsScreen(snapshot = snapshot, battery = batteryTelemetry)
                }
                composable(DroidScopeDestination.HISTORY.route) {
                    HistoryScreen(battery = batteryTelemetry)
                }
                composable(DroidScopeDestination.SETTINGS.route) {
                    SettingsScreen(
                        snapshot = snapshot,
                        battery = batteryTelemetry,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryOverviewContent(
    b: BatteryTelemetry,
    tempSeries: List<Float>,
    powerSeries: List<Float>
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(
                title = "Telemetria Real da Bateria",
                lines = listOf(
                    "Nível: ${b.levelPercent}% (${b.statusLabel})",
                    "Saúde (Sistema): ${b.healthLabel}",
                    "Tecnologia: ${b.technology}",
                    "Temperatura: ${b.temperatureC?.let { String.format(Locale.US, "%.1f °C", it) } ?: "Indisponível"}",
                    "Tensão: ${b.voltageV?.let { String.format(Locale.US, "%.3f V", it) } ?: "Indisponível"}",
                    "Corrente instantânea: ${b.currentNowMicroAmps?.let { "${it / 1000} mA ($it µA)" } ?: "Indisponível"}",
                    "Carga atual (ChargeCounter): ${b.chargeCounterMicroAh?.let { "${it / 1000} mAh" } ?: "Indisponível"}",
                    "Potência estimada: ${b.estimatedPowerWatts?.let { String.format(Locale.US, "%.2f W", it) } ?: "Indisponível"}",
                    "Ciclos (Android 14+): ${b.cycleCount?.toString() ?: "Não exposto pelo dispositivo"}"
                )
            )
        }
        item {
            RealtimeLineChartCard(
                title = "Temperatura da Bateria",
                unit = "°C",
                values = tempSeries,
                lineColor = MaterialTheme.colorScheme.secondary
            )
        }
        if (powerSeries.isNotEmpty()) {
            item {
                RealtimeLineChartCard(
                    title = "Potência Estimada",
                    unit = "W",
                    values = powerSeries,
                    lineColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HardwareOverviewContent(s: DeviceSnapshot, adv: AdvancedHardwareSnapshot) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(
                title = "Processador & Sistema",
                lines = listOf(
                    "Dispositivo: ${s.manufacturer} ${s.model}",
                    "SoC: ${s.socModel}",
                    "Hardware: ${s.hardware}",
                    "Arquitetura: ${s.cpuArchitecture}",
                    "ABIs: ${s.supportedAbis.joinToString(", ")}",
                    "Sistema: ${s.androidVersion}",
                    "Kernel: ${s.kernelVersion}",
                    "Patch de segurança: ${s.securityPatch}"
                )
            )
        }
        item {
            InfoCard(
                title = "Tela & Memória",
                lines = listOf(
                    "Resolução: ${s.displayResolution} (${s.densityDpi} dpi)",
                    "Taxa de atualização: ${String.format(Locale.US, "%.1f Hz", s.refreshRateHz)}",
                    "HDR suportado: ${if (adv.display.hdrTypes.isEmpty()) "Nenhum detectado" else adv.display.hdrTypes.joinToString(", ")}",
                    "Brilho atual: ${adv.display.brightnessPercent?.let { "$it%" } ?: "Indisponível"}",
                    "RAM: ${String.format(Locale.US, "%.2f / %d GB", s.ramUsedGb, s.ramGb)}",
                    "Armazenamento: ${String.format(Locale.US, "%.2f / %d GB", s.storageUsedGb, s.storageGb)}"
                )
            )
        }
        if (adv.cameras.isNotEmpty()) {
            item {
                Text("Câmeras (${adv.cameras.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(adv.cameras) { cam ->
                InfoCard(
                    title = "Câmera #${cam.id} (${cam.facing})",
                    lines = listOf(
                        "Resolução do sensor: ${cam.megapixels?.let { String.format(Locale.US, "%.1f MP", it) } ?: "N/D"}",
                        "Tamanho físico: ${cam.sensorSizeMm ?: "N/D"}",
                        "Distância focal: ${if (cam.focalLengthsMm.isEmpty()) "N/D" else cam.focalLengthsMm.joinToString(", ") { "${it}mm" }}",
                        "Abertura: ${if (cam.apertures.isEmpty()) "N/D" else cam.apertures.joinToString(", ") { "f/$it" }}",
                        "Estabilização óptica (OIS): ${if (cam.hasOis) "Sim" else "Não"}"
                    )
                )
            }
        }
    }
}

@Composable
private fun MonitoringOverviewContent(
    s: DeviceSnapshot,
    adv: AdvancedHardwareSnapshot,
    ramSeries: List<Float>,
    cpuFreqSeries: List<Float>
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RealtimeLineChartCard(
                title = "Uso de Memória RAM",
                unit = "%",
                values = ramSeries,
                lineColor = MaterialTheme.colorScheme.primary
            )
        }
        if (cpuFreqSeries.isNotEmpty()) {
            item {
                RealtimeLineChartCard(
                    title = "Frequência Média da CPU",
                    unit = "MHz",
                    values = cpuFreqSeries,
                    lineColor = MaterialTheme.colorScheme.secondary
                )
            }
        }
        item {
            InfoCard(
                title = "Frequências por Núcleo (Tempo Real)",
                lines = if (s.cpuCoresInfo.isEmpty()) {
                    listOf("Acesso a /sys/devices/system/cpu restrito neste dispositivo.")
                } else {
                    s.cpuCoresInfo.map { c ->
                        val cur = c.currentFrequencyMhz?.let { "$it MHz" } ?: "N/D"
                        val max = c.maximumFrequencyMhz?.let { "$it MHz" } ?: "N/D"
                        "Core ${c.core}: $cur (Máx: $max)"
                    }
                }
            )
        }
        item {
            val net = adv.network
            InfoCard(
                title = "Rede, Conectividade & Tráfego",
                lines = listOf(
                    "Conexão: ${s.connectionStatus}",
                    "IP Local: ${net.localIp}",
                    "Link estimado: ↓ ${net.downstreamMbps?.let { String.format(Locale.US, "%.1f Mbps", it) } ?: "N/D"} | ↑ ${net.upstreamMbps?.let { String.format(Locale.US, "%.1f Mbps", it) } ?: "N/D"}",
                    "Bluetooth: ${net.bluetoothStatus} | NFC: ${net.nfcStatus}",
                    "Tráfego total (desde boot): ↓ ${net.totalRxMb?.let { String.format(Locale.US, "%.1f MB", it) } ?: "N/D"} | ↑ ${net.totalTxMb?.let { String.format(Locale.US, "%.1f MB", it) } ?: "N/D"}",
                    "Tempo de atividade: ${s.uptimeHours} h"
                )
            )
        }
    }
}

@Composable
private fun SensorsOverviewContent(s: DeviceSnapshot) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(s.sensors) { sensor ->
            LiveSensorCard(sensorInfo = sensor)
        }
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
