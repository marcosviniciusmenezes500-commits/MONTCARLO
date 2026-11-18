package com.droidscope.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

enum class DroidScopeDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    BATTERY("battery", "Bateria", Icons.Default.BatteryChargingFull),
    BENCHMARK("benchmark", "Benchmark", Icons.Default.Speed),
    HARDWARE("hardware", "Hardware", Icons.Default.DeveloperBoard),
    MONITORING("monitoring", "Monitoramento", Icons.Default.MonitorHeart),
    SENSORS("sensors", "Sensores", Icons.Default.Sensors),
    DIAGNOSTICS("diagnostics", "Diagnóstico", Icons.Default.HealthAndSafety),
    HISTORY("history", "Histórico", Icons.Default.History),
    SETTINGS("settings", "Configurações", Icons.Default.Settings)
}
