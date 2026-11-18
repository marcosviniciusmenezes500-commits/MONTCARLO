package com.droidscope.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun RealtimeLineChartCard(
    title: String,
    unit: String,
    values: List<Float>,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val latest = values.lastOrNull()
    val minVal = values.minOrNull() ?: 0f
    val maxVal = values.maxOrNull() ?: 1f
    val range = (maxVal - minVal).takeIf { it > 0.001f } ?: 1f

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = latest?.let { String.format(Locale.US, "%.1f %s", it, unit) } ?: "N/D",
                    style = MaterialTheme.typography.titleMedium,
                    color = lineColor
                )
            }

            if (values.size >= 2) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val stepX = size.width / (values.size - 1).coerceAtLeast(1)
                    val path = Path()
                    values.forEachIndexed { idx, v ->
                        val x = idx * stepX
                        val normY = (v - minVal) / range
                        val y = size.height - (normY * size.height * 0.85f) - (size.height * 0.075f)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    // Linhas guia sutis
                    drawLine(
                        color = lineColor.copy(alpha = 0.15f),
                        start = Offset(0f, size.height * 0.5f),
                        end = Offset(size.width, size.height * 0.5f),
                        strokeWidth = 1f
                    )
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "Mín: %.1f %s", minVal, unit),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = String.format(Locale.US, "Máx: %.1f %s", maxVal, unit),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Text(
                    text = "Coletando amostras em tempo real...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
