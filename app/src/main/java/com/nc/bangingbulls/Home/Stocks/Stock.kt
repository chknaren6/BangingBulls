package com.nc.bangingbulls.Home.Stocks


import com.google.firebase.Timestamp

data class Stock(
    val id: String = "",
    val name: String = "",
    val symbol: String = "",
    val totalSupply: Long = 0,
    val availableSupply: Long = 0,
    val price: Double = 0.0,
    val priceHistory: List<PricePoint> = emptyList(),           // today only
    val lastWeekHistory: Map<String, List<PricePoint>> = emptyMap(), // yyyy-MM-dd -> points
    val likes: Long = 0,
    val dislikes: Long = 0,
    val investorsCount: Long = 0,
    val description: String = "",
    val updatedAt: Timestamp? = null
)
data class PricePoint(
    val ts: Long = 0L,
    val price: Double = 0.0
)


data class Holding(
    val stockId: String = "",
    val qty: Long = 0L,
    val avgPrice: Double = 0.0
)

data class Trade(
    val uid: String = "",
    val stockId: String = "",
    val qty: Long = 0L,
    val price: Double = 0.0,
    val type: String = "",
    val ts: Long = 0L
)