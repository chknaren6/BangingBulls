package com.nc.bangingbulls.Home.Stocks

import com.nc.bangingbulls.stocks.StocksRepository

suspend fun seedExampleStocks(repo: StocksRepository) {
    val now = System.currentTimeMillis()
    val ts = { System.currentTimeMillis() }
    val sample = listOf(
        Stock(
            id = "tesla",
            name = "Tesla",
            symbol = "TSLA",
            totalSupply = 1000,
            availableSupply = 1000,
            price = 250.0,
            priceHistory = listOf(PricePoint(ts(), 250.0)),
            description = "Electric vehicle maker"
        ),
        Stock(
            id = "apple",
            name = "Apple",
            symbol = "AAPL",
            totalSupply = 1000,
            availableSupply = 1000,
            price = 175.0,
            priceHistory = listOf(PricePoint(ts(), 175.0)),
            description = "Consumer electronics"
        )
    )
    sample.forEach { repo.addStock(it) }
}
