package com.droidscope.app.diagnostics

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.droidscope.app.data.BatteryTelemetry
import com.droidscope.core.database.entity.DiagnosticEventEntity
import com.droidscope.core.domain.DeviceSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun analyze(snapshot: DeviceSnapshot, battery: BatteryTelemetry): List<DiagnosticEventEntity> {
        val now = System.currentTimeMillis()
        val findings = mutableListOf<DiagnosticEventEntity>()

        // 1. Temperatura da bateria
        battery.temperatureC?.let { temp ->
            when {
                temp >= 45.0 -> findings += DiagnosticEventEntity(
                    timestampMs = now,
                    title = "Temperatura crítica da bateria",
                    description = "A bateria está operando acima de 45 °C, o que acelera a degradação química.",
                    severity = "ALTA",
                    evidence = String.format(Locale.US, "Temperatura medida: %.1f °C", temp),
                    recommendation = "Interrompa cargas pesadas/carregamento rápido e aguarde o resfriamento."
                )
                temp >= 40.0 -> findings += DiagnosticEventEntity(
                    timestampMs = now,
                    title = "Aquecimento elevado da bateria",
                    description = "A temperatura da bateria está acima da faixa ideal de operação (< 40 °C).",
                    severity = "MÉDIA",
                    evidence = String.format(Locale.US, "Temperatura medida: %.1f °C", temp),
                    recommendation = "Evite usar o aparelho enquanto carrega ou sob exposição solar direta."
                )
            }
        }

        // 2. Thermal Throttling (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val thermalStatus = pm.currentThermalStatus
            if (thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE) {
                val label = when (thermalStatus) {
                    PowerManager.THERMAL_STATUS_MODERATE -> "Moderado"
                    PowerManager.THERMAL_STATUS_SEVERE -> "Severo"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Crítico"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergência"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Desligamento iminente"
                    else -> "Elevado"
                }
                findings += DiagnosticEventEntity(
                    timestampMs = now,
                    title = "Thermal Throttling ativo",
                    description = "O sistema operacional reduziu desempenho para conter o aquecimento.",
                    severity = if (thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) "ALTA" else "MÉDIA",
                    evidence = "PowerManager.currentThermalStatus = $label ($thermalStatus)",
                    recommendation = "Feche aplicativos intensivos em CPU/GPU antes de executar benchmarks."
                )
            }
        }

        // 3. Memória RAM
        if (snapshot.ramGb > 0) {
            val ramUsagePct = (snapshot.ramUsedGb / snapshot.ramGb.toDouble()) * 100.0
            if (ramUsagePct >= 85.0) {
                findings += DiagnosticEventEntity(
                    timestampMs = now,
                    title = "Uso elevado de memória RAM",
                    description = "Pouca memória livre disponível; o Android pode encerrar processos em segundo plano.",
                    severity = "MÉDIA",
                    evidence = String.format(Locale.US, "%.2f GB usados de %d GB (%.0f%%)", snapshot.ramUsedGb, snapshot.ramGb, ramUsagePct),
                    recommendation = "Feche aplicativos pesados em segundo plano para melhorar a fluidez."
                )
            }
        }

        // 4. Armazenamento
        if (snapshot.storageGb > 0) {
            val freePct = (snapshot.storageAvailableGb / snapshot.storageGb.toDouble()) * 100.0
            if (freePct <= 10.0) {
                findings += DiagnosticEventEntity(
                    timestampMs = now,
                    title = "Armazenamento próximo do limite",
                    description = "Menos de 10% do armazenamento interno está livre, impactando velocidade de I/O.",
                    severity = "ALTA",
                    evidence = String.format(Locale.US, "%.2f GB livres de %d GB (%.1f%%)", snapshot.storageAvailableGb, snapshot.storageGb, freePct),
                    recommendation = "Libere espaço removendo caches, vídeos antigos ou aplicativos não utilizados."
                )
            }
        }

        // 5. Saúde reportada pelo sistema
        if (battery.healthLabel == "Degradada" || battery.healthLabel == "Superaquecida" || battery.healthLabel == "Sobretensão") {
            findings += DiagnosticEventEntity(
                timestampMs = now,
                title = "Alerta de saúde da bateria",
                description = "O controlador do sistema reportou estado anormal para a bateria.",
                severity = "ALTA",
                evidence = "BatteryManager.EXTRA_HEALTH = ${battery.healthLabel}",
                recommendation = "Verifique o carregador/cabo utilizado e avalie assistência técnica."
            )
        }

        return findings
    }
}
