package com.nc.bangingbulls.Home.Stocks


data class PortfolioItem(
    val name: String,
    val symbol: String,
    val qty: Long,
    val currentPrice: Double,
    val avgPrice: Double
)
