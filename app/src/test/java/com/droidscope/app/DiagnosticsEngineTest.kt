package com.droidscope.app

import android.content.Context
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.app.diagnostics.DiagnosticsEngine
import com.droidscope.core.domain.DeviceSnapshot
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsEngineTest {

    @Test
    fun analyze_flagsHighBatteryTempAndLowStorage() {
        val context = mockk<Context>(relaxed = true)
        val engine = DiagnosticsEngine(context)

        val snapshot = DeviceSnapshot(
            manufacturer = "Test",
            model = "Device",
            androidVersion = "Android 14",
            processor = "SoC (8 núcleos)",
            ramGb = 8,
            ramUsedGb = 7.2, // 90% RAM
            ramAvailableGb = 0.8,
            storageGb = 100,
            storageUsedGb = 95.0,
            storageAvailableGb = 5.0, // 5% livre
            batteryPercent = 40,
            batteryTempC = 46.0,
            batteryVoltageV = 3.8,
            isCharging = true,
            connectionStatus = "Wi-Fi",
            uptimeHours = 12,
            socModel = "TestSoC",
            hardware = "qcom",
            cpuArchitecture = "aarch64",
            supportedAbis = listOf("arm64-v8a"),
            cpuCoresInfo = emptyList(),
            displayResolution = "1080 x 2400",
            densityDpi = 420,
            refreshRateHz = 120f,
            kernelVersion = "6.1",
            securityPatch = "2026-10-01",
            sensors = emptyList()
        )

        val battery = BatteryTelemetry(
            levelPercent = 40,
            isCharging = true,
            statusLabel = "Carregando",
            healthLabel = "Boa",
            technology = "Li-ion",
            temperatureC = 46.0,
            voltageV = 3.8,
            currentNowMicroAmps = 2_000_000,
            chargeCounterMicroAh = 1_800_000,
            estimatedPowerWatts = 7.6,
            cycleCount = 50
        )

        val findings = engine.analyze(snapshot, battery)
        assertTrue(findings.any { it.title.contains("Temperatura crítica") })
        assertTrue(findings.any { it.title.contains("RAM") })
        assertTrue(findings.any { it.title.contains("Armazenamento") })
    }
}
