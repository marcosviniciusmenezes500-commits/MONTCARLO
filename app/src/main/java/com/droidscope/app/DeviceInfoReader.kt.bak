package com.droidscope.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import com.droidscope.core.domain.CpuCoreInfo
import com.droidscope.core.domain.DeviceSnapshot
import com.droidscope.core.domain.SensorInfo
import java.io.File
import kotlin.math.roundToInt

private const val GB = 1024.0 * 1024.0 * 1024.0

private fun readFrequency(
    file: File
): Int? {
    return try {
        file.readText()
            .trim()
            .toLongOrNull()
            ?.div(1000L)
            ?.toInt()
            ?.takeIf { it > 0 }
    } catch (_: Exception) {
        null
    }
}

private fun readCpuCores(): List<CpuCoreInfo> {
    val cpuDirectory = File("/sys/devices/system/cpu")

    val directories = cpuDirectory.listFiles()
        ?.filter {
            it.isDirectory &&
                Regex("cpu[0-9]+").matches(it.name)
        }
        ?.sortedBy {
            it.name.removePrefix("cpu").toIntOrNull() ?: 0
        }
        ?: emptyList()

    return directories.map { directory ->

        val frequencyDirectory =
            File(directory, "cpufreq")

        val current = readFrequency(
            File(frequencyDirectory, "scaling_cur_freq")
        ) ?: readFrequency(
            File(frequencyDirectory, "cpuinfo_cur_freq")
        )

        val maximum = readFrequency(
            File(frequencyDirectory, "cpuinfo_max_freq")
        ) ?: readFrequency(
            File(frequencyDirectory, "scaling_max_freq")
        )

        CpuCoreInfo(
            core = directory.name.removePrefix("cpu").toInt(),
            currentFrequencyMhz = current,
            maximumFrequencyMhz = maximum
        )
    }
}

fun readDeviceSnapshot(context: Context): DeviceSnapshot {

    // PROCESSADOR

    val cpuCores = Runtime.getRuntime().availableProcessors()

    val soc = if (Build.VERSION.SDK_INT >= 31) {
        Build.SOC_MODEL.takeIf { it.isNotBlank() }
            ?: "Indisponível"
    } else {
        "Não disponibilizado nesta versão"
    }

    val processor = if (soc != "Indisponível" &&
        soc != "Não disponibilizado nesta versão"
    ) {
        soc
    } else {
        Build.HARDWARE
    }

    val architecture = System.getProperty("os.arch")
        ?: "Indisponível"

    val cpuCoreInfo = readCpuCores()

    // MEMÓRIA RAM

    val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE)
                as ActivityManager

    val memoryInfo = ActivityManager.MemoryInfo()

    activityManager.getMemoryInfo(memoryInfo)

    val ramTotal = memoryInfo.totalMem / GB
    val ramAvailable = memoryInfo.availMem / GB
    val ramUsed = (memoryInfo.totalMem - memoryInfo.availMem) / GB

    // ARMAZENAMENTO

    val stat = StatFs(Environment.getDataDirectory().path)

    val storageTotal = stat.totalBytes / GB
    val storageAvailable = stat.availableBytes / GB
    val storageUsed = storageTotal - storageAvailable

    // BATERIA

    val batteryIntent = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )

    val level = batteryIntent?.getIntExtra(
        BatteryManager.EXTRA_LEVEL, -1
    ) ?: -1

    val scale = batteryIntent?.getIntExtra(
        BatteryManager.EXTRA_SCALE, -1
    ) ?: -1

    val batteryPercent =
        if (level >= 0 && scale > 0) {
            level * 100 / scale
        } else {
            -1
        }

    val temperature = batteryIntent?.getIntExtra(
        BatteryManager.EXTRA_TEMPERATURE, -1
    ) ?: -1

    val voltage = batteryIntent?.getIntExtra(
        BatteryManager.EXTRA_VOLTAGE, -1
    ) ?: -1

    val status = batteryIntent?.getIntExtra(
        BatteryManager.EXTRA_STATUS,
        BatteryManager.BATTERY_STATUS_UNKNOWN
    ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

    val charging =
        status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    // CONECTIVIDADE

    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

    val network = connectivityManager.activeNetwork

    val capabilities =
        connectivityManager.getNetworkCapabilities(network)

    val connectionStatus = when {
        capabilities == null -> "Offline"

        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI
        ) -> "Wi-Fi"

        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR
        ) -> "Rede móvel"

        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_ETHERNET
        ) -> "Ethernet"

        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_VPN
        ) -> "VPN"

        else -> "Conectado"
    }

    // TELA

    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE)
                as WindowManager

    val displayMetrics = DisplayMetrics()

    @Suppress("DEPRECATION")
    val display = windowManager.defaultDisplay

    @Suppress("DEPRECATION")
    display.getRealMetrics(displayMetrics)

    val resolution =
        "${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}"

    val density = displayMetrics.densityDpi

    val refreshRate = display.refreshRate

    // SENSORES

    val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE)
                as SensorManager

    val sensors = sensorManager
        .getSensorList(android.hardware.Sensor.TYPE_ALL)
        .map { sensor ->

            SensorInfo(
                name = sensor.name,
                vendor = sensor.vendor,
                type = sensor.stringType,
                resolution = sensor.resolution,
                maximumRange = sensor.maximumRange,
                power = sensor.power,
                version = sensor.version
            )
        }

    // SISTEMA

    val kernelVersion =
        System.getProperty("os.version")
            ?: "Indisponível"

    val securityPatch =
        if (Build.VERSION.SDK_INT >= 23) {
            Build.VERSION.SECURITY_PATCH
                ?: "Indisponível"
        } else {
            "Indisponível"
        }

    // TEMPO DE ATIVIDADE

    val uptimeHours =
        SystemClock.elapsedRealtime() / 3_600_000L

    // RESULTADO

    return DeviceSnapshot(

        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        androidVersion =
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",

        processor = "$processor ($cpuCores núcleos)",

        ramGb = ramTotal.roundToInt(),
        ramUsedGb = ramUsed,
        ramAvailableGb = ramAvailable,

        storageGb = storageTotal.roundToInt(),
        storageUsedGb = storageUsed,
        storageAvailableGb = storageAvailable,

        batteryPercent = batteryPercent,

        batteryTempC =
            if (temperature >= 0) temperature / 10.0 else -1.0,

        batteryVoltageV =
            if (voltage >= 0) voltage / 1000.0 else -1.0,

        isCharging = charging,

        connectionStatus = connectionStatus,
        uptimeHours = uptimeHours.toInt(),

        socModel = soc,
        hardware = Build.HARDWARE,
        cpuArchitecture = architecture,
        supportedAbis = Build.SUPPORTED_ABIS.toList(),
        cpuCoresInfo = cpuCoreInfo,

        displayResolution = resolution,
        densityDpi = density,
        refreshRateHz = refreshRate,

        kernelVersion = kernelVersion,
        securityPatch = securityPatch,

        sensors = sensors
    )
}
