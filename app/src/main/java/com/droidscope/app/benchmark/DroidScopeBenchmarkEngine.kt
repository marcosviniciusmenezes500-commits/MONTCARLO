package com.droidscope.app.benchmark

import android.content.Context
import com.droidscope.app.data.BatteryMetricsReader
import com.droidscope.core.database.entity.BenchmarkResultEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class BenchmarkProgress(
    val stage: String,
    val fraction: Float
)

@Singleton
class DroidScopeBenchmarkEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryMetricsReader: BatteryMetricsReader
) {
    suspend fun runFullSuite(
        onProgress: (BenchmarkProgress) -> Unit
    ): BenchmarkResultEntity = withContext(Dispatchers.Default) {
        val startBattery = batteryMetricsReader.read()
        val cores = max(1, Runtime.getRuntime().availableProcessors())

        onProgress(BenchmarkProgress("CPU Single-Core (Inteiros, Float, SHA-256, Compressão)", 0.15f))
        val singleCoreMs = measureCpuWorkload()
        val singleCoreScore = ((120_000.0 / max(1.0, singleCoreMs.toDouble()))).roundToInt()

        onProgress(BenchmarkProgress("CPU Multi-Core ($cores threads)", 0.40f))
        val multiCoreMs = measureMultiCoreWorkload(cores)
        val multiCoreScore = ((120_000.0 * cores / max(1.0, multiCoreMs.toDouble()))).roundToInt()

        onProgress(BenchmarkProgress("Memória RAM (Leitura/Escrita 16 MB)", 0.65f))
        val memoryMbPerSec = measureMemoryBandwidth()
        val memoryScore = (memoryMbPerSec * 0.5).roundToInt().coerceAtLeast(1)

        onProgress(BenchmarkProgress("Armazenamento (I/O Sequencial 16 MB)", 0.85f))
        val (writeMbSec, readMbSec) = withContext(Dispatchers.IO) {
            measureStorageSequentialIo()
        }
        val storageScore = (((writeMbSec + readMbSec) / 2.0) * 2.0).roundToInt().coerceAtLeast(1)

        val endBattery = batteryMetricsReader.read()
        onProgress(BenchmarkProgress("Concluído", 1.0f))

        val details = buildString {
            append("{\"engine\":\"DroidScope Native v1\",")
            append("\"cores\":$cores,")
            append("\"singleCoreMs\":$singleCoreMs,")
            append("\"multiCoreMs\":$multiCoreMs,")
            append("\"memoryMbSec\":${"%.1f".format(memoryMbPerSec)},")
            append("\"storageWriteMbSec\":${"%.1f".format(writeMbSec)},")
            append("\"storageReadMbSec\":${"%.1f".format(readMbSec)}}")
        }

        BenchmarkResultEntity(
            timestampMs = System.currentTimeMillis(),
            suiteName = "DroidScope Full Suite",
            singleCoreScore = singleCoreScore,
            multiCoreScore = multiCoreScore,
            memoryScore = memoryScore,
            storageScore = storageScore,
            startTempC = startBattery.temperatureC,
            endTempC = endBattery.temperatureC,
            batteryPercent = endBattery.levelPercent,
            detailsJson = details
        )
    }

    private fun measureCpuWorkload(): Long {
        val start = System.nanoTime()

        // 1. Inteiros: contagem de primos até 60.000
        var primeCount = 0
        for (n in 2..60_000) {
            var isPrime = true
            var d = 2
            while (d * d <= n) {
                if (n % d == 0) {
                    isPrime = false
                    break
                }
                d++
            }
            if (isPrime) primeCount++
        }

        // 2. Ponto flutuante: operações trigonométricas e raiz
        var acc = primeCount.toDouble()
        for (i in 1..150_000) {
            val x = i * 0.001
            acc += sin(x) * cos(x) + sqrt(x)
        }

        // 3. Criptografia: SHA-256 repetido
        val md = MessageDigest.getInstance("SHA-256")
        var hash = acc.toString().toByteArray()
        repeat(4_000) {
            hash = md.digest(hash)
        }

        // 4. Compressão: Deflater em buffer de 256 KB
        val input = ByteArray(256 * 1024) { i -> ((i * 31 + hash[i % hash.size]) and 0xFF).toByte() }
        val output = ByteArray(input.size)
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
        deflater.setInput(input)
        deflater.finish()
        deflater.deflate(output)
        deflater.end()

        return (System.nanoTime() - start) / 1_000_000L
    }

    private suspend fun measureMultiCoreWorkload(cores: Int): Long = coroutineScope {
        val start = System.nanoTime()
        val jobs = (0 until cores).map {
            async(Dispatchers.Default) { measureCpuWorkload() }
        }
        jobs.awaitAll()
        (System.nanoTime() - start) / 1_000_000L
    }

    private fun measureMemoryBandwidth(): Double {
        val sizeBytes = 16 * 1024 * 1024 // 16 MB
        val ints = IntArray(sizeBytes / 4)
        val iterations = 6
        val start = System.nanoTime()

        var checksum = 0L
        repeat(iterations) { pass ->
            for (i in ints.indices) {
                ints[i] = i xor pass
            }
            for (i in ints.indices) {
                checksum += ints[i]
            }
        }

        val elapsedSec = max(1e-6, (System.nanoTime() - start) / 1_000_000_000.0)
        // 16 MB escritos + 16 MB lidos por iteração (mantém checksum vivo)
        val totalMb = (sizeBytes.toDouble() * 2.0 * iterations) / (1024.0 * 1024.0)
        return if (checksum == Long.MIN_VALUE) 0.0 else totalMb / elapsedSec
    }

    private fun measureStorageSequentialIo(): Pair<Double, Double> {
        val tempFile = File(context.cacheDir, "droidscope_io_bench.tmp")
        val totalBytes = 16 * 1024 * 1024 // 16 MB
        val chunk = ByteArray(64 * 1024) { i -> (i and 0xFF).toByte() }

        try {
            val writeStart = System.nanoTime()
            FileOutputStream(tempFile).use { fos ->
                var remaining = totalBytes
                while (remaining > 0) {
                    val toWrite = minOf(chunk.size, remaining)
                    fos.write(chunk, 0, toWrite)
                    remaining -= toWrite
                }
                fos.flush()
                fos.fd.sync()
            }
            val writeSec = max(1e-6, (System.nanoTime() - writeStart) / 1_000_000_000.0)

            val readStart = System.nanoTime()
            var totalRead = 0L
            FileInputStream(tempFile).use { fis ->
                while (true) {
                    val r = fis.read(chunk)
                    if (r <= 0) break
                    totalRead += r
                }
            }
            val readSec = max(1e-6, (System.nanoTime() - readStart) / 1_000_000_000.0)

            val mb = totalRead / (1024.0 * 1024.0)
            return Pair(mb / writeSec, mb / readSec)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }
}
