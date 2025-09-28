package com.nc.bangingbulls.Home

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Portfolio DTO
data class PortfolioLine(
    val stockId: String,
    val symbol: String,
    val name: String,
    val qty: Long,
    val avgPrice: Double,
    val currentPrice: Double,
    val value: Double
)

// Read full portfolio for a user (coins + lines)
suspend fun loadUserPortfolio(uid: String): Pair<Double, List<PortfolioLine>> {
    val db = FirebaseFirestore.getInstance()
    val userDoc = db.collection("users").document(uid).get().await()
    val coins = (userDoc.getDouble("coins") ?: userDoc.getLong("coins")?.toDouble() ?: 0.0)

    val holdings = db.collection("users").document(uid).collection("holdings").get().await().documents
    val prices = db.collection("stocks").get().await().documents.associateBy({ it.id }) {
        Triple(
            it.getString("symbol") ?: it.id,
            it.getString("name") ?: (it.getString("symbol") ?: it.id),
            it.getDouble("price") ?: 0.0
        )
    }

    val lines = holdings.mapNotNull { h ->
        val stockId = h.getString("stockId") ?: return@mapNotNull null
        val qty = h.getLong("qty") ?: 0L
        val avg = h.getDouble("avgPrice") ?: 0.0
        val triple = prices[stockId] ?: Triple(stockId, stockId, 0.0)
        val symbol = triple.first
        val name = triple.second
        val current = triple.third
        PortfolioLine(stockId, symbol, name, qty, avg, current, current * qty)
    }.sortedByDescending { it.value }

    return coins to lines
}

// Add or increase a holding explicitly (not via trade)
suspend fun addOrIncreaseHolding(uid: String, stockId: String, addQty: Long, avgPrice: Double? = null) {
    require(addQty > 0)
    val db = FirebaseFirestore.getInstance()
    val holdingRef = db.collection("users").document(uid).collection("holdings").document(stockId)
    val snap = holdingRef.get().await()
    if (snap.exists()) {
        val oldQty = snap.getLong("qty") ?: 0L
        val oldAvg = snap.getDouble("avgPrice") ?: (avgPrice ?: 0.0)
        val newQty = oldQty + addQty
        val newAvg = if (avgPrice == null) oldAvg else ((oldAvg * oldQty) + (avgPrice * addQty)) / newQty
        holdingRef.update(mapOf("qty" to newQty, "avgPrice" to newAvg)).await()
    } else {
        val priceToUse = avgPrice ?: 0.0
        holdingRef.set(mapOf("stockId" to stockId, "qty" to addQty, "avgPrice" to priceToUse)).await()
    }
}

// Delete a holding (or set qty to 0) explicitly
suspend fun deleteHolding(uid: String, stockId: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(uid).collection("holdings").document(stockId).delete().await()
}
