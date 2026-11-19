package com.droidscope.app.benchmark

import android.content.Context
import android.os.Build
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
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Random
import java.util.zip.Deflater
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class BenchmarkCategory(val label: String) {
    FULL("Suite Completa"),
    CPU_ONLY("Somente CPU"),
    MEMORY_ONLY("Somente Memória"),
    STORAGE_ONLY("Somente Armazenamento"),
    GPU_ONLY("Somente GPU")
}

data class BenchmarkProgress(
    val stage: String,
    val fraction: Float
)

@Singleton
class DroidScopeBenchmarkEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryMetricsReader: BatteryMetricsReader,
    private val gpuBenchmarkRunner: GpuBenchmarkRunner
) {
    suspend fun runSuite(
        category: BenchmarkCategory = BenchmarkCategory.FULL,
        onProgress: (BenchmarkProgress) -> Unit
    ): BenchmarkResultEntity = withContext(Dispatchers.Default) {
        val startBattery = batteryMetricsReader.read()
        val cores = max(1, Runtime.getRuntime().availableProcessors())

        var singleCoreScore: Int? = null
        var multiCoreScore: Int? = null
        var memoryScore: Int? = null
        var storageScore: Int? = null
        var gpuScore: Int? = null

        var singleCoreMs = 0L
        var multiCoreMs = 0L
        var memoryMbPerSec = 0.0
        var memoryLatencyNs = 0.0
        var seqWriteMbSec = 0.0
        var seqReadMbSec = 0.0
        var randReadIops = 0.0
        var randWriteIops = 0.0
        var gpuFps: Double? = null

        if (category == BenchmarkCategory.FULL || category == BenchmarkCategory.CPU_ONLY) {
            onProgress(BenchmarkProgress("CPU Single-Core (Inteiros, Float, SHA-256, Compressão)", 0.15f))
            singleCoreMs = measureCpuWorkload()
            singleCoreScore = calculateCpuScore(singleCoreMs, threads = 1)

            onProgress(BenchmarkProgress("CPU Multi-Core ($cores threads)", 0.35f))
            multiCoreMs = measureMultiCoreWorkload(cores)
            multiCoreScore = calculateCpuScore(multiCoreMs, threads = cores)
        }

        if (category == BenchmarkCategory.FULL || category == BenchmarkCategory.MEMORY_ONLY) {
            onProgress(BenchmarkProgress("Memória RAM (Banda 16 MB + Latência)", 0.55f))
            memoryMbPerSec = measureMemoryBandwidth()
            memoryLatencyNs = measureMemoryLatencyNs()
            val latencyBonus = (500.0 / max(1.0, memoryLatencyNs)).coerceAtMost(500.0)
            memoryScore = (memoryMbPerSec * 0.5 + latencyBonus).roundToInt().coerceAtLeast(1)
        }

        if (category == BenchmarkCategory.FULL || category == BenchmarkCategory.STORAGE_ONLY) {
            onProgress(BenchmarkProgress("Armazenamento (Sequencial + Aleatório R/W 4 KB)", 0.75f))
            val io = withContext(Dispatchers.IO) { measureStorageIo() }
            seqWriteMbSec = io[0]
            seqReadMbSec = io[1]
            randReadIops = io[2]
            randWriteIops = io[3]
            storageScore = (((seqWriteMbSec + seqReadMbSec) / 2.0) * 1.5 + ((randReadIops + randWriteIops) * 0.03))
                .roundToInt()
                .coerceAtLeast(1)
        }

        if (category == BenchmarkCategory.FULL || category == BenchmarkCategory.GPU_ONLY) {
            onProgress(BenchmarkProgress("GPU (OpenGL ES Offscreen Shader)", 0.90f))
            val gpuResult = gpuBenchmarkRunner.runOffscreenShaderBenchmark()
            gpuScore = gpuResult?.score
            gpuFps = gpuResult?.fps
        }

        val endBattery = batteryMetricsReader.read()
        val overallScore = calculateOverallScore(singleCoreScore, multiCoreScore, memoryScore, storageScore, gpuScore)
        onProgress(BenchmarkProgress("Concluído", 1.0f))

        val details = buildString {
            append("{\"engine\":\"DroidScope Native v3\",")
            append("\"device\":\"${Build.MANUFACTURER} ${Build.MODEL}\",")
            append("\"overallScore\":${overallScore ?: "null"},")
            append("\"cores\":$cores,")
            append("\"singleCoreMs\":$singleCoreMs,")
            append("\"multiCoreMs\":$multiCoreMs,")
            append("\"memoryMbSec\":${"%.1f".format(memoryMbPerSec)},")
            append("\"memoryLatencyNs\":${"%.1f".format(memoryLatencyNs)},")
            append("\"storageSeqWriteMbSec\":${"%.1f".format(seqWriteMbSec)},")
            append("\"storageSeqReadMbSec\":${"%.1f".format(seqReadMbSec)},")
            append("\"storageRandReadIops\":${"%.0f".format(randReadIops)},")
            append("\"storageRandWriteIops\":${"%.0f".format(randWriteIops)},")
            append("\"gpuFps\":${gpuFps?.let { "%.1f".format(it) } ?: "null"}}")
        }

        BenchmarkResultEntity(
            timestampMs = System.currentTimeMillis(),
            suiteName = "DroidScope — ${category.label}${overallScore?.let { " (Geral: $it pts)" } ?: ""}",
            singleCoreScore = singleCoreScore,
            multiCoreScore = multiCoreScore,
            memoryScore = memoryScore,
            storageScore = storageScore,
            gpuScore = gpuScore,
            startTempC = startBattery.temperatureC,
            endTempC = endBattery.temperatureC,
            batteryPercent = endBattery.levelPercent,
            detailsJson = details
        )
    }

    private fun measureCpuWorkload(): Long {
        val start = System.nanoTime()
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

        var acc = primeCount.toDouble()
        for (i in 1..150_000) {
            val x = i * 0.001
            acc += sin(x) * cos(x) + sqrt(x)
        }

        val md = MessageDigest.getInstance("SHA-256")
        var hash = acc.toString().toByteArray()
        repeat(4_000) { hash = md.digest(hash) }

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
        val jobs = (0 until cores).map { async(Dispatchers.Default) { measureCpuWorkload() } }
        jobs.awaitAll()
        (System.nanoTime() - start) / 1_000_000L
    }

    private fun measureMemoryBandwidth(): Double {
        val sizeBytes = 16 * 1024 * 1024
        val ints = IntArray(sizeBytes / 4)
        val iterations = 6
        val start = System.nanoTime()

        var checksum = 0L
        repeat(iterations) { pass ->
            for (i in ints.indices) ints[i] = i xor pass
            for (i in ints.indices) checksum += ints[i]
        }

        val elapsedSec = max(1e-6, (System.nanoTime() - start) / 1_000_000_000.0)
        val totalMb = (sizeBytes.toDouble() * 2.0 * iterations) / (1024.0 * 1024.0)
        return if (checksum == Long.MIN_VALUE) 0.0 else totalMb / elapsedSec
    }

    private fun measureMemoryLatencyNs(): Double {
        val size = 1_048_576 // 4 MB array para exceder L1/L2
        val next = IntArray(size) { i -> (i * 1103515245 + 12345) and (size - 1) }
        val hops = 500_000
        var idx = 0
        val start = System.nanoTime()
        repeat(hops) { idx = next[idx] }
        val elapsedNs = (System.nanoTime() - start).toDouble()
        return if (idx == Int.MIN_VALUE) 0.0 else elapsedNs / hops
    }

    private fun measureStorageIo(): DoubleArray {
        val tempFile = File(context.cacheDir, "droidscope_io_bench.tmp")
        val totalBytes = 16 * 1024 * 1024
        val chunk = ByteArray(64 * 1024) { i -> (i and 0xFF).toByte() }
        val block4k = ByteArray(4096) { i -> (i and 0xFF).toByte() }

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

            val ops = 300
            val maxBlocks = (totalBytes / 4096) - 1
            val rng = Random(42L)
            var randReadSec = 1.0
            var randWriteSec = 1.0

            RandomAccessFile(tempFile, "rw").use { raf ->
                val rrStart = System.nanoTime()
                repeat(ops) {
                    raf.seek(rng.nextInt(maxBlocks).toLong() * 4096L)
                    raf.readFully(block4k)
                }
                randReadSec = max(1e-6, (System.nanoTime() - rrStart) / 1_000_000_000.0)

                val rwStart = System.nanoTime()
                repeat(ops) {
                    raf.seek(rng.nextInt(maxBlocks).toLong() * 4096L)
                    raf.write(block4k)
                }
                randWriteSec = max(1e-6, (System.nanoTime() - rwStart) / 1_000_000_000.0)
            }

            val mb = totalRead / (1024.0 * 1024.0)
            return doubleArrayOf(mb / writeSec, mb / readSec, ops / randReadSec, ops / randWriteSec)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    companion object {
        fun calculateCpuScore(elapsedMs: Long, threads: Int): Int {
            return ((120_000.0 * max(1, threads)) / max(1.0, elapsedMs.toDouble())).roundToInt()
        }

        fun calculateOverallScore(
            single: Int?,
            multi: Int?,
            memory: Int?,
            storage: Int?,
            gpu: Int?
        ): Int? {
            val values = listOfNotNull(single, multi, memory, storage, gpu)
            return if (values.isEmpty()) null else values.average().roundToInt()
        }
    }
}
