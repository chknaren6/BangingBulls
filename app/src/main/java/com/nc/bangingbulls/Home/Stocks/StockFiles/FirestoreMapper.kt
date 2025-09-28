package com.nc.bangingbulls.Home.Stocks.StockFiles

import com.google.firebase.firestore.DocumentSnapshot
import kotlin.collections.get


fun DocumentSnapshot.toStock(): Stock {
    val id = id
    val name = getString("name") ?: ""
    val symbol = getString("symbol") ?: ""
    val totalSupply = getLong("totalSupply") ?: 0L
    val availableSupply = getLong("availableSupply") ?: totalSupply
    val price = getDouble("price") ?: 0.0
    val likes = getLong("likes") ?: 0L
    val dislikes = getLong("dislikes") ?: 0L
    val investorsCount = getLong("investorsCount") ?: 0L
    val description = getString("description") ?: ""
    val updatedAt = getTimestamp("updatedAt")

    val ph = mutableListOf<PricePoint>()
    val todayHistoryRaw = get("priceHistory") as? List<*>
    todayHistoryRaw?.forEach { item ->
        val m = item as? Map<*, *>
        val ts = (m?.get("ts") as? Number)?.toLong() ?: 0L
        val p = (m?.get("price") as? Number)?.toDouble() ?: 0.0
        ph.add(PricePoint(ts, p))
    }

    val lwhRaw = get("lastWeekHistory") as? Map<*, *>
    val lwh = mutableMapOf<String, List<PricePoint>>()
    lwhRaw?.forEach { (k, v) ->
        val day = k as? String ?: return@forEach
        val list = (v as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<*, *>
            val ts = (m?.get("ts") as? Number)?.toLong() ?: return@mapNotNull null
            val p = (m["price"] as? Number)?.toDouble() ?: return@mapNotNull null
            PricePoint(ts, p)
        } ?: emptyList()
        lwh[day] = list
    }

    return Stock(
        id, name, symbol, totalSupply, availableSupply, price,
        priceHistory = ph, lastWeekHistory = lwh,
        likes = likes, dislikes = dislikes, investorsCount = investorsCount,
        description = description, updatedAt = updatedAt
    )
}
