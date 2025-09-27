package com.nc.bangingbulls.Home.Stocks

import android.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
@Composable
fun SimpleLineChart(points: List<Double>, modifier: Modifier = Modifier.height(180.dp)) {
    if (points.isEmpty()) { Box(modifier = modifier) { Text("No data") }; return }
    val max = points.maxOrNull() ?: 1.0
    val min = points.minOrNull() ?: 0.0
    Canvas(modifier = modifier.fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1).coerceAtLeast(1)
        val normalized = points.map { ((it - min) / (max - min + 1e-9)).toFloat() } // 0..1

        val path = Path()
        normalized.forEachIndexed { i, v ->
            val x = (i * stepX).toFloat()
            val y = h - (v * h)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = Color(0xFF00C853), style = Stroke(width = 3f))

        normalized.forEachIndexed { i, v ->
            val x = (i * stepX).toFloat()
            val y = h - (v * h)
            drawCircle(Color.White, radius = 3f, center = Offset(x, y))
        }
    }
}
