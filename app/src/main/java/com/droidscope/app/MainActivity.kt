package com.droidscope.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidscope.app.data.BatteryMetricsReader
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.app.data.DeviceRepository
import com.droidscope.app.ui.navigation.DroidScopeAppScaffold
import com.droidscope.app.ui.theme.DroidScopeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var batteryMetricsReader: BatteryMetricsReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }

            DroidScopeTheme(darkTheme = isDarkTheme) {
                val snapshot by deviceRepository
                    .observeDeviceSnapshot(intervalMs = 2000L)
                    .collectAsStateWithLifecycle(initialValue = null)

                val battery by produceState<BatteryTelemetry?>(initialValue = null) {
                    while (true) {
                        value = withContext(Dispatchers.IO) { batteryMetricsReader.read() }
                        delay(2000L)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentSnapshot = snapshot
                    val currentBattery = battery
                    if (currentSnapshot != null && currentBattery != null) {
                        DroidScopeAppScaffold(
                            snapshot = currentSnapshot,
                            batteryTelemetry = currentBattery,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = it }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
