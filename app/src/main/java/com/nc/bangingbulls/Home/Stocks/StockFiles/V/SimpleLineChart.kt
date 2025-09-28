package com.nc.bangingbulls.Home.Stocks.StockFiles.V

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.PricePoint
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.Stock

@Composable
fun SimpleLineChart(prices: List<Double>) {
    Canvas(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)) {
        if (prices.size < 2) return@Canvas
        val maxPrice = prices.maxOrNull() ?: 0.0
        val minPrice = prices.minOrNull() ?: 0.0
        val w = size.width / (prices.size - 1)
        val h = size.height

        for (i in 0 until prices.size - 1) {
            val startX = i * w
            val startY = h - ((prices[i] - minPrice) / (maxPrice - minPrice) * h).toFloat()
            val endX = (i + 1) * w
            val endY = h - ((prices[i + 1] - minPrice) / (maxPrice - minPrice) * h).toFloat()

            val color = when {
                prices[i + 1] > prices[i] -> Color(0xFF1B5E20)
                prices[i + 1] < prices[i] -> Color(0xFFD32F2F)
                else -> Color(0xFF1976D2)
            }
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f
            )
        }
    }
}

fun mergedLifetimePoints(stock: Stock): List<PricePoint> {
    val fromWeek =
        stock.lastWeekHistory.keys.sorted().flatMap { day -> stock.lastWeekHistory[day].orEmpty() }
    return fromWeek + stock.priceHistory
}

