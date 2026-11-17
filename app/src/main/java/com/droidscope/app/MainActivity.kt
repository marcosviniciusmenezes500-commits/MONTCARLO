package com.droidscope.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.droidscope.app.ui.theme.DroidScopeTheme
import com.droidscope.feature.dashboard.DashboardScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            DroidScopeTheme {

                var snapshot by remember {
                    mutableStateOf(
                        readDeviceSnapshot(this@MainActivity)
                    )
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(5000)
                        snapshot = readDeviceSnapshot(
                            this@MainActivity
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(snapshot = snapshot)
                }
            }
        }
    }
}