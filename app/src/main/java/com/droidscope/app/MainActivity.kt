package com.droidscope.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.droidscope.app.ui.theme.DroidScopeTheme
import com.droidscope.feature.dashboard.DashboardScreen
import com.droidscope.core.domain.DeviceSnapshot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DroidScopeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val snapshot = DeviceSnapshot(
                        manufacturer = "Google",
                        model = "Pixel 9 Pro",
                        androidVersion = "Android 15",
                        processor = "Tensor G4",
                        ramGb = 12,
                        storageGb = 256,
                        batteryPercent = 86,
                        batteryTempC = 31.6,
                        isCharging = true,
                        connectionStatus = "5G",
                        uptimeHours = 18
                    )
                    DashboardScreen(snapshot = snapshot)
                }
            }
        }
    }
}
