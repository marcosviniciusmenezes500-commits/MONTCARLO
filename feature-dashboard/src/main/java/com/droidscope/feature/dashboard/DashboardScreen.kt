package com.droidscope.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "DroidScope",
            style = MaterialTheme.typography.headlineMedium
        )

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Device overview")
                Text(text = "${snapshot.manufacturer} ${snapshot.model}")
                Text(text = "${snapshot.androidVersion}")
                Text(text = "CPU: ${snapshot.processor}")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
DashboardStatCard(
    label = "RAM",
    value = "${snapshot.ramGb} GB",
    modifier = Modifier.weight(1f)
)

DashboardStatCard(
    label = "Storage",
    value = "${snapshot.storageGb} GB",
    modifier = Modifier.weight(1f)
)

DashboardStatCard(
    label = "Battery",
    value = "${snapshot.batteryPercent}%",
    modifier = Modifier.weight(1f)
)
        }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Live telemetry")
                Text("Battery temp: ${snapshot.batteryTempC}°C")
                Text("Charging: ${if (snapshot.isCharging) "Yes" else "No"}")
                Text("Network: ${snapshot.connectionStatus}")
                Text("Uptime: ${snapshot.uptimeHours} hours")
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
