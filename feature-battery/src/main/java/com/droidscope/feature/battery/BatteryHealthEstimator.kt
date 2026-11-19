package com.droidscope.feature.battery

import kotlin.math.abs
import kotlin.math.roundToInt

data class BatteryMeasurementPoint(
    val timestampMs: Long,
    val levelPercent: Int,
    val currentMicroAmps: Int?,
    val chargeCounterMicroAh: Int?,
    val isCharging: Boolean
)

enum class ConfidenceLevel(val label: String) {
    INSUFFICIENT("Insuficiente"),
    LOW("Baixa"),
    MEDIUM("Média"),
    HIGH("Alta")
}

data class BatteryHealthEstimate(
    val estimatedCapacityMah: Int?,
    val designCapacityMah: Int?,
    val healthPercent: Double?,
    val degradationPercent: Double?,
    val confidence: ConfidenceLevel,
    val methodology: String,
    val totalDeltaPercentObserved: Int,
    val sampleCount: Int
)

object BatteryHealthEstimator {

    /**
     * Estima capacidade real integrando variações consecutivas ao longo do tempo:
     * 1) Prioriza delta de ChargeCounter (µAh) entre pares consecutivos com Δ% >= 1.
     * 2) Fallback: integra corrente média × tempo (∫ I dt) quando ChargeCounter não está disponível.
     * O nível de confiança cresce conforme o total de Δ% acumulado e o número de amostras.
     */
    fun estimate(
        points: List<BatteryMeasurementPoint>,
        designCapacityMah: Int?
    ): BatteryHealthEstimate {
        if (points.size < 2) {
            return BatteryHealthEstimate(
                estimatedCapacityMah = null,
                designCapacityMah = designCapacityMah,
                healthPercent = null,
                degradationPercent = null,
                confidence = ConfidenceLevel.INSUFFICIENT,
                methodology = "Aguardando pelo menos 2 medições com variação de carga para calcular capacidade real.",
                totalDeltaPercentObserved = 0,
                sampleCount = points.size
            )
        }

        val sorted = points.sortedBy { it.timestampMs }
        var accumulatedMah = 0.0
        var accumulatedDeltaPct = 0
        var usedChargeCounter = 0
        var usedCurrentIntegration = 0

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            val deltaPct = abs(curr.levelPercent - prev.levelPercent)
            val dtHours = (curr.timestampMs - prev.timestampMs) / 3_600_000.0

            // Ignora intervalos inválidos ou maiores que 6h (evita distorção por gaps longos)
            if (deltaPct < 1 || dtHours <= 0.0 || dtHours > 6.0) continue

            val ccPrev = prev.chargeCounterMicroAh
            val ccCurr = curr.chargeCounterMicroAh
            if (ccPrev != null && ccCurr != null && ccPrev > 0 && ccCurr > 0) {
                val deltaMah = abs(ccCurr - ccPrev) / 1000.0
                if (deltaMah > 5.0) {
                    accumulatedMah += deltaMah
                    accumulatedDeltaPct += deltaPct
                    usedChargeCounter++
                    continue
                }
            }

            val iPrev = prev.currentMicroAmps
            val iCurr = curr.currentMicroAmps
            if (iPrev != null && iCurr != null) {
                val avgMa = (abs(iPrev) + abs(iCurr)) / 2000.0
                val deltaMah = avgMa * dtHours
                if (deltaMah > 5.0) {
                    accumulatedMah += deltaMah
                    accumulatedDeltaPct += deltaPct
                    usedCurrentIntegration++
                }
            }
        }

        if (accumulatedDeltaPct < 2 || accumulatedMah <= 0.0) {
            return BatteryHealthEstimate(
                estimatedCapacityMah = null,
                designCapacityMah = designCapacityMah,
                healthPercent = null,
                degradationPercent = null,
                confidence = ConfidenceLevel.INSUFFICIENT,
                methodology = "Variação acumulada de carga (< 2%) ainda insuficiente para estimativa confiável.",
                totalDeltaPercentObserved = accumulatedDeltaPct,
                sampleCount = sorted.size
            )
        }

        val estimatedFullMah = (accumulatedMah / (accumulatedDeltaPct / 100.0)).roundToInt()
        val confidence = when {
            accumulatedDeltaPct >= 80 && sorted.size >= 20 -> ConfidenceLevel.HIGH
            accumulatedDeltaPct >= 25 && sorted.size >= 8 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val methodLabel = when {
            usedChargeCounter > 0 && usedCurrentIntegration > 0 ->
                "Híbrida: ΔChargeCounter ($usedChargeCounter) + ∫I·dt ($usedCurrentIntegration) em Δ$accumulatedDeltaPct% acumulados."
            usedChargeCounter > 0 ->
                "ΔChargeCounter acumulado ($usedChargeCounter intervalos, Δ$accumulatedDeltaPct% observados)."
            else ->
                "Integração de corrente × tempo ∫I·dt ($usedCurrentIntegration intervalos, Δ$accumulatedDeltaPct% observados)."
        }

        val healthPct = designCapacityMah?.takeIf { it > 0 }?.let {
            ((estimatedFullMah.toDouble() / it.toDouble()) * 100.0).coerceIn(0.0, 120.0)
        }
        val degradationPct = healthPct?.let { (100.0 - it).coerceAtLeast(0.0) }

        return BatteryHealthEstimate(
            estimatedCapacityMah = estimatedFullMah,
            designCapacityMah = designCapacityMah,
            healthPercent = healthPct,
            degradationPercent = degradationPct,
            confidence = confidence,
            methodology = methodLabel,
            totalDeltaPercentObserved = accumulatedDeltaPct,
            sampleCount = sorted.size
        )
    }
}
