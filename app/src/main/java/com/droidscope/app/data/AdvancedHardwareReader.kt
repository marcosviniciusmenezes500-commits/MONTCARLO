package com.droidscope.app.data

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CameraHardwareInfo(
    val id: String,
    val facing: String,
    val megapixels: Double?,
    val focalLengthsMm: List<Float>,
    val apertures: List<Float>,
    val hasOis: Boolean,
    val sensorSizeMm: String?
)

data class DisplayAdvancedInfo(
    val hdrTypes: List<String>,
    val brightnessPercent: Int?
)

data class NetworkAdvancedInfo(
    val localIp: String,
    val downstreamMbps: Double?,
    val upstreamMbps: Double?,
    val bluetoothStatus: String,
    val nfcStatus: String,
    val totalRxMb: Double?,
    val totalTxMb: Double?
)

data class AdvancedHardwareSnapshot(
    val cameras: List<CameraHardwareInfo>,
    val display: DisplayAdvancedInfo,
    val network: NetworkAdvancedInfo
)

@Singleton
class AdvancedHardwareReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun read(): AdvancedHardwareSnapshot {
        return AdvancedHardwareSnapshot(
            cameras = readCameras(),
            display = readDisplayAdvanced(),
            network = readNetworkAdvanced()
        )
    }

    private fun readCameras(): List<CameraHardwareInfo> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return emptyList()
        return try {
            manager.cameraIdList.mapNotNull { id ->
                try {
                    val chars = manager.getCameraCharacteristics(id)
                    val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
                        CameraCharacteristics.LENS_FACING_BACK -> "Traseira"
                        CameraCharacteristics.LENS_FACING_FRONT -> "Frontal"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externa"
                        else -> "Desconhecida"
                    }
                    val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                    val mp = pixelArray?.let { (it.width.toLong() * it.height.toLong()) / 1_000_000.0 }
                    val focals = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
                    val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()
                    val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                    val hasOis = oisModes?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON } == true
                    val phys = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    val sensorMm = phys?.let { String.format(Locale.US, "%.2f x %.2f mm", it.width, it.height) }

                    CameraHardwareInfo(
                        id = id,
                        facing = facing,
                        megapixels = mp,
                        focalLengthsMm = focals,
                        apertures = apertures,
                        hasOis = hasOis,
                        sensorSizeMm = sensorMm
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun readDisplayAdvanced(): DisplayAdvancedInfo {
        val hdrTypes = mutableListOf<String>()
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display ?: wm.defaultDisplay
            } else {
                wm.defaultDisplay
            }
            val caps = display?.hdrCapabilities?.supportedHdrTypes
            caps?.forEach { type ->
                hdrTypes += when (type) {
                    Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                    Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                    Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                    Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                    else -> "HDR ($type)"
                }
            }
        } catch (_: Exception) {
            // Ignora se indisponível
        }

        val brightnessPct = try {
            val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            ((raw / 255.0) * 100.0).toInt().coerceIn(0, 100)
        } catch (_: Exception) {
            null
        }

        return DisplayAdvancedInfo(
            hdrTypes = hdrTypes.distinct(),
            brightnessPercent = brightnessPct
        )
    }

    private fun readNetworkAdvanced(): NetworkAdvancedInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val linkProps = cm.getLinkProperties(activeNetwork)

        val localIp = linkProps?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
            ?: "Indisponível"

        val downMbps = caps?.linkDownstreamBandwidthKbps?.takeIf { it > 0 }?.let { it / 1000.0 }
        val upMbps = caps?.linkUpstreamBandwidthKbps?.takeIf { it > 0 }?.let { it / 1000.0 }

        val btStatus = try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter
            when {
                adapter == null -> "Não suportado"
                adapter.isEnabled -> "Ativado"
                else -> "Desativado"
            }
        } catch (_: SecurityException) {
            "Restrito por permissão"
        } catch (_: Exception) {
            "Indisponível"
        }

        val nfcStatus = try {
            val nfc = NfcAdapter.getDefaultAdapter(context)
            when {
                nfc == null -> "Não suportado"
                nfc.isEnabled -> "Ativado"
                else -> "Desativado"
            }
        } catch (_: Exception) {
            "Indisponível"
        }

        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        val rxMb = if (rxBytes != TrafficStats.UNSUPPORTED.toLong() && rxBytes >= 0) rxBytes / (1024.0 * 1024.0) else null
        val txMb = if (txBytes != TrafficStats.UNSUPPORTED.toLong() && txBytes >= 0) txBytes / (1024.0 * 1024.0) else null

        return NetworkAdvancedInfo(
            localIp = localIp,
            downstreamMbps = downMbps,
            upstreamMbps = upMbps,
            bluetoothStatus = btStatus,
            nfcStatus = nfcStatus,
            totalRxMb = rxMb,
            totalTxMb = txMb
        )
    }
}
