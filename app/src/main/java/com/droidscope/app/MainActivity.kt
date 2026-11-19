package com.droidscope.app

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidscope.app.data.AdvancedHardwareReader
import com.droidscope.app.data.AdvancedHardwareSnapshot
import com.droidscope.app.data.BatteryMetricsReader
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.app.data.DeviceRepository
import com.droidscope.app.data.UserPreferences
import com.droidscope.app.data.UserPreferencesRepository
import com.droidscope.app.ui.navigation.DroidScopeAppScaffold
import com.droidscope.app.ui.theme.DroidScopeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var batteryMetricsReader: BatteryMetricsReader

    @Inject
    lateinit var advancedHardwareReader: AdvancedHardwareReader

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val prefs by preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            val scope = rememberCoroutineScope()

            DroidScopeTheme(darkTheme = prefs.isDarkTheme) {
                val snapshot by deviceRepository
                    .observeDeviceSnapshot(intervalMs = prefs.samplingIntervalMs)
                    .collectAsStateWithLifecycle(initialValue = null)

                val battery by produceState<BatteryTelemetry?>(initialValue = null, key1 = prefs.samplingIntervalMs) {
                    while (true) {
                        value = withContext(Dispatchers.IO) { batteryMetricsReader.read() }
                        delay(prefs.samplingIntervalMs)
                    }
                }

                val advanced by produceState<AdvancedHardwareSnapshot?>(initialValue = null, key1 = prefs.samplingIntervalMs) {
                    while (true) {
                        value = withContext(Dispatchers.IO) { advancedHardwareReader.read() }
                        delay(prefs.samplingIntervalMs * 2)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentSnapshot = snapshot
                    val currentBattery = battery
                    val currentAdvanced = advanced
                    if (currentSnapshot != null && currentBattery != null && currentAdvanced != null) {
                        DroidScopeAppScaffold(
                            snapshot = currentSnapshot,
                            batteryTelemetry = currentBattery,
                            advancedHardware = currentAdvanced,
                            isDarkTheme = prefs.isDarkTheme,
                            onToggleTheme = { enabled ->
                                scope.launch { preferencesRepository.setDarkTheme(enabled) }
                            }
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
