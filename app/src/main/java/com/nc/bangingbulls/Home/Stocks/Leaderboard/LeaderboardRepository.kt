package com.nc.bangingbulls.Home.Stocks.Leaderboard

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LeaderboardRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun recomputeForCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        val userDoc = db.collection("users").document(uid).get().await()
        val coins = (userDoc.getDouble("coins") ?: userDoc.getLong("coins")?.toDouble() ?: 0.0)

        val holdings = db.collection("users").document(uid).collection("holdings").get().await().documents
        val stockPrices = db.collection("stocks").get().await().documents.associateBy({ it.id }) {
            it.getDouble("price") ?: 0.0
        }

        var portfolioValue = 0.0
        for (h in holdings) {
            val stockId = h.getString("stockId") ?: continue
            val qty = h.getLong("qty") ?: 0L
            val price = stockPrices[stockId] ?: 0.0
            portfolioValue += price * qty
        }
        val total = coins + portfolioValue

        db.collection("leaderboard").document(uid).set(
            mapOf(
                "uid" to uid,
                "username" to (userDoc.getString("username") ?: ""),
                "coins" to coins,
                "portfolio" to portfolioValue,
                "totalCoins" to total,
                "ts" to System.currentTimeMillis()
            )
        ).await()
    }
}