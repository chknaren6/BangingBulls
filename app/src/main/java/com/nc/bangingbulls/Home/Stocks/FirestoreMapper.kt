package com.nc.bangingbulls.Home.Stocks

import com.google.firebase.firestore.DocumentSnapshot
fun DocumentSnapshot.toStock(): Stock {
    val id = id
    val name = getString("name") ?: ""
    val symbol = getString("symbol") ?: ""
    val totalSupply = getLong("totalSupply") ?: 0L
    val availableSupply = getLong("availableSupply") ?: totalSupply
    val price = getDouble("price") ?: 0.0
    val description = getString("description") ?: ""
    val likes = getLong("likes") ?: 0L
    val dislikes = getLong("dislikes") ?: 0L
    val investorsCount = getLong("investorsCount") ?: 0L
    val updatedAt = getTimestamp("updatedAt")

    val ph = mutableListOf<PricePoint>()

    // Safely reading 'history' or 'priceHistory'
    val rawHistory = get("history") as? List<*>
    rawHistory?.forEach { item ->
        val mapItem = item as? Map<*, *>
        if (mapItem != null) {
            val ts = (mapItem["time"] as? Number)?.toLong() ?: 0L
            val p = (mapItem["price"] as? Number)?.toDouble() ?: 0.0
            ph.add(PricePoint(ts, p))
        }
    }

    return Stock(
        id = id,
        name = name,
        symbol = symbol,
        totalSupply = totalSupply,
        availableSupply = availableSupply,
        price = price,
        priceHistory = ph,
        likes = likes,
        dislikes = dislikes,
        investorsCount = investorsCount,
        description = description,
        updatedAt = updatedAt
    )
}
