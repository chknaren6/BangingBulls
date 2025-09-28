package com.nc.bangingbulls.Home.Stocks.StockFiles.M

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.Stocks.Comments.M.Comment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.collections.get
import kotlin.math.max
import kotlin.math.round
import kotlin.random.Random

class StocksRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val stocksCol = db.collection("stocks")
    private val usersCol = db.collection("users")
    private val tradesCol = db.collection("trades")

    // Live list of stocks
    fun observeStocks() = callbackFlow<List<Stock>> {
        val sub = stocksCol.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { it.toStock() } ?: emptyList()
            trySend(list)
        }
        awaitClose { sub.remove() }
    }

    // Observe single stock
    fun observeStock(stockId: String) = callbackFlow<Stock?> {
        val docRef = stocksCol.document(stockId)
        val sub = docRef.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.toStock())
        }
        awaitClose { sub.remove() }
    }

    // Add initial stock (admin or dev)
    suspend fun addStock(stock: Stock) {
        val doc = stocksCol.document(stock.id.ifBlank { stock.symbol.lowercase() })
        val payload = mapOf(
            "name" to stock.name,
            "symbol" to stock.symbol,
            "totalSupply" to stock.totalSupply,
            "availableSupply" to stock.availableSupply,
            "price" to stock.price,
            "priceHistory" to stock.priceHistory.map { mapOf("ts" to it.ts, "price" to it.price) },
            "likes" to stock.likes,
            "dislikes" to stock.dislikes,
            "investorsCount" to stock.investorsCount,
            "description" to stock.description,
            "updatedAt" to Timestamp.Companion.now()
        )
        doc.set(payload).await()
    }

    // Basic like/dislike increment
    suspend fun likeStock(stockId: String, delta: Long = 1) {
        stocksCol.document(stockId).update("likes", FieldValue.increment(delta)).await()
    }
    suspend fun dislikeStock(stockId: String, delta: Long = 1) {
        stocksCol.document(stockId).update("dislikes", FieldValue.increment(delta)).await()
    }

    // Add a comment under the stock
    suspend fun addComment(stockId: String, comment: Comment) {
        val comments = stocksCol.document(stockId).collection("comments")
        val doc = comments.document()
        val payload = mapOf(
            "authorUid" to comment.userId,
            "authorName" to comment.username,
            "text" to comment.text,
            "ts" to comment.ts
        )
        doc.set(payload).await()
    }
    private fun impactPriceOnBuy(price: Double, qty: Long): Double {
        val impact = 1.0 + (0.0008 * qty).coerceAtMost(0.15)
        return round(price * impact * 100.0) / 100.0
    }
    private fun impactPriceOnSell(price: Double, qty: Long): Double {
        val impact = 1.0 - (0.0008 * qty).coerceAtLeast(-0.15)
        return round(price * impact * 100.0) / 100.0
    }// StocksRepository.kt

    suspend fun buyStockTransactional(stockId: String, qty: Long): Result<Unit> {
        if (qty <= 0) return Result.failure(IllegalArgumentException("Quantity must be > 0"))
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)
        val stockRef = db.collection("stocks").document(stockId)
        val holdingRef = userRef.collection("holdings").document(stockId)
        val holderRef = stockRef.collection("holders").document(uid) // membership flag
        val tradesCol = db.collection("trades")

        return try {
            db.runTransaction { tx ->
                // READS FIRST
                val userSnap = tx.get(userRef)
                val stockSnap = tx.get(stockRef)
                val holdingSnap = tx.get(holdingRef)
                val holderSnap = tx.get(holderRef)

                val userCoins = (userSnap.getDouble("coins")
                    ?: userSnap.getLong("coins")?.toDouble()
                    ?: 0.0)

                val price = stockSnap.getDouble("price")
                    ?: throw IllegalStateException("Stock has no price")

                val available = stockSnap.getLong("availableSupply")
                    ?: stockSnap.getLong("totalSupply")
                    ?: 0L

                if (available < qty) throw IllegalStateException("Not enough supply")
                val totalCost = price * qty
                if (userCoins < totalCost) throw IllegalStateException("Not enough coins")

                // Compute new states
                val newUserCoins = userCoins - totalCost

                val (newQty, newAvg) = if (holdingSnap.exists()) {
                    val oldQty = holdingSnap.getLong("qty") ?: 0L
                    val oldAvg = holdingSnap.getDouble("avgPrice") ?: price
                    val nQty = oldQty + qty
                    val nAvg = ((oldAvg * oldQty) + (price * qty)) / nQty
                    nQty to nAvg
                } else {
                    qty to price
                }

                val newAvailable = (available - qty).coerceAtLeast(0L)

                val impact = (1.0 + (0.0008 * qty).coerceAtMost(0.15))
                val newPrice = round(price * impact * 100.0) / 100.0
                val pricePoint = mapOf("ts" to System.currentTimeMillis(), "price" to newPrice)

                // WRITES
                tx.update(userRef, "coins", newUserCoins)

                if (holdingSnap.exists()) {
                    tx.update(holdingRef, mapOf("qty" to newQty, "avgPrice" to newAvg, "stockId" to stockId))
                } else {
                    tx.set(holdingRef, mapOf("stockId" to stockId, "qty" to newQty, "avgPrice" to newAvg))
                }

                // Create membership if not present and bump investorsCount once
                if (!holderSnap.exists()) {
                    tx.set(holderRef, mapOf("since" to Timestamp.now()))
                    tx.update(stockRef, "investorsCount", FieldValue.increment(1))
                }

                tx.update(stockRef, "availableSupply", newAvailable)
                tx.update(
                    stockRef,
                    mapOf(
                        "price" to newPrice,
                        "updatedAt" to Timestamp.now(),
                        "priceHistory" to FieldValue.arrayUnion(pricePoint)
                    )
                )

                val tradeDoc = tradesCol.document()
                tx.set(
                    tradeDoc,
                    mapOf(
                        "uid" to uid,
                        "stockId" to stockId,
                        "qty" to qty,
                        "price" to price,
                        "type" to "buy",
                        "ts" to System.currentTimeMillis()
                    )
                )

                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    suspend fun sellStockTransactional(stockId: String, qty: Long): Result<Unit> {
        if (qty <= 0) return Result.failure(IllegalArgumentException("Quantity must be > 0"))
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)
        val stockRef = db.collection("stocks").document(stockId)
        val holdingRef = userRef.collection("holdings").document(stockId)
        val holderRef = stockRef.collection("holders").document(uid)
        val tradesCol = db.collection("trades")

        return try {
            db.runTransaction { tx ->
                // READS
                val userSnap = tx.get(userRef)
                val stockSnap = tx.get(stockRef)
                val holdingSnap = tx.get(holdingRef)
                val holderSnap = tx.get(holderRef)

                if (!holdingSnap.exists()) throw IllegalStateException("No holding")
                val haveQty = holdingSnap.getLong("qty") ?: 0L
                if (haveQty < qty) throw IllegalStateException("Insufficient holding")

                val price = stockSnap.getDouble("price")
                    ?: throw IllegalStateException("Stock has no price")

                val available = stockSnap.getLong("availableSupply") ?: 0L

                // Compute
                val revenue = price * qty
                val userCoins = (userSnap.getDouble("coins") ?: userSnap.getLong("coins")?.toDouble() ?: 0.0)
                val newUserCoins = userCoins + revenue

                val leftQty = haveQty - qty
                val newAvailable = available + qty

                val impact = (1.0 - (0.0008 * qty).coerceAtMost(0.15))
                val newPrice = round(price * impact * 100.0) / 100.0
                val pricePoint = mapOf("ts" to System.currentTimeMillis(), "price" to newPrice)

                // WRITES
                tx.update(userRef, "coins", newUserCoins)

                if (leftQty <= 0L) {
                    tx.delete(holdingRef)
                    if (holderSnap.exists()) {
                        tx.delete(holderRef)
                        tx.update(stockRef, "investorsCount", FieldValue.increment(-1))
                    }
                } else {
                    tx.update(holdingRef, "qty", leftQty)
                }

                tx.update(stockRef, "availableSupply", newAvailable)
                tx.update(
                    stockRef,
                    mapOf(
                        "price" to newPrice,
                        "updatedAt" to Timestamp.now(),
                        "priceHistory" to FieldValue.arrayUnion(pricePoint)
                    )
                )

                val tradeDoc = tradesCol.document()
                tx.set(
                    tradeDoc,
                    mapOf(
                        "uid" to uid,
                        "stockId" to stockId,
                        "qty" to qty,
                        "price" to price,
                        "type" to "sell",
                        "ts" to System.currentTimeMillis()
                    )
                )

                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Compute leaderboard = coins + sum(currentPrice * qty) across all holdings
    suspend fun recomputeLeaderboardForAllUsers() {
        val db = FirebaseFirestore.getInstance()
        val users = db.collection("users").get().await().documents

        val stocksMap = db.collection("stocks").get().await().documents.associateBy({ it.id }) {
            it.getDouble("price") ?: 0.0
        }

        val batch = db.batch()
        for (u in users) {
            val uid = u.id
            val coins = (u.getDouble("coins") ?: u.getLong("coins")?.toDouble() ?: 0.0)
            val holdings = db.collection("users").document(uid).collection("holdings").get().await().documents

            var portfolioValue = 0.0
            for (h in holdings) {
                val stockId = h.getString("stockId") ?: continue
                val qty = h.getLong("qty") ?: 0L
                val currentPrice = stocksMap[stockId] ?: 0.0
                portfolioValue += currentPrice * qty
            }
            val total = coins + portfolioValue

            val lbRef = db.collection("leaderboard").document(uid)
            batch.set(lbRef, mapOf(
                "uid" to uid,
                "username" to (u.getString("username") ?: ""),
                "coins" to coins,
                "portfolio" to portfolioValue,
                "totalCoins" to total,
                "ts" to System.currentTimeMillis()
            ))
        }
        batch.commit().await()
    }
    suspend fun tickEconomy() {
        val now = ZonedDateTime.now()
        val hour = now.hour
        if (hour < 5 || hour > 22) return

        val snap = stocksCol.get().await()
        val end = System.currentTimeMillis()
        val start = end - 15L * 60L * 1000L

        for (doc in snap.documents) {
            val stockId = doc.id
            val symbol = doc.getString("symbol") ?: continue
            val currentPrice = doc.getDouble("price") ?: continue
            val momentum = doc.getDouble("socialMomentum") ?: 0.0

            val recentTrades = tradesCol.whereEqualTo("stockId", stockId).whereGreaterThan("ts", start).get().await()
            var buys = 0L; var sells = 0L
            for (t in recentTrades.documents) {
                val type = t.getString("type") ?: ""
                val qty = t.getLong("qty") ?: 0L
                if (type == "buy") buys += qty else if (type == "sell") sells += qty
            }

            val newPrice = calcNextPrice(symbol, hour, currentPrice, buys, sells, momentum)
            val point = mapOf("ts" to end, "price" to newPrice)

            // decay momentum by 10% every tick so effects fade
            val decayed = momentum * 0.9

            stocksCol.document(stockId).update(
                mapOf(
                    "price" to newPrice,
                    "priceHistory" to FieldValue.arrayUnion(point),
                    "updatedAt" to Timestamp.now(),
                    "socialMomentum" to decayed
                )
            ).await()
        }
    }


    // Nightly at 23:59: move today's priceHistory to lastWeekHistory[yyyy-MM-dd], keep only last 7 keys, then clear priceHistory
    suspend fun archiveTodayToLastWeek() {
        val today = LocalDate.now()
        val dayKey = today.toString() // yyyy-MM-dd
        val snap = stocksCol.get().await()
        for (doc in snap.documents) {
            val priceHistory = (doc.get("priceHistory") as? List<*>)?.mapNotNull { m ->
                (m as? Map<*, *>)?.let {
                    val ts = (it["ts"] as? Number)?.toLong() ?: return@mapNotNull null
                    val p = (it["price"] as? Number)?.toDouble() ?: return@mapNotNull null
                    mapOf("ts" to ts, "price" to p)
                }
            } ?: emptyList()

            val lastWeek = (doc.get("lastWeekHistory") as? Map<*, *>)?.mapKeys { it.key.toString() }?.toMutableMap() ?: mutableMapOf()
            lastWeek[dayKey] = priceHistory

            // trim to 7 most recent days by key sort
            val keys = lastWeek.keys.sorted().takeLast(7)
            val trimmed = keys.associateWith { (lastWeek[it] as? List<*>) ?: emptyList<Any>() }

            stocksCol.document(doc.id).update(
                mapOf(
                    "lastWeekHistory" to trimmed,
                    "priceHistory" to emptyList<Map<String, Any>>()
                )
            ).await()
        }
    }

    // One-time generator to backfill last 7 days of history with artificial movement
    suspend fun generateLastWeekHistoryBaseline() {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val snap = stocksCol.get().await()
        for (doc in snap.documents) {
            val symbol = doc.getString("symbol") ?: continue
            var base = doc.getDouble("price") ?: 10.0
            val result = mutableMapOf<String, List<Map<String, Any>>>()

            for (d in 6 downTo 1) {
                val date = today.minusDays(d.toLong())
                val points = mutableListOf<Map<String, Any>>()
                var price = base
                for (h in 5..22) {
                    for (m in listOf(0, 15, 30, 45)) {
                        val ts = date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
                        price = calcNextPrice(symbol, h, price, 0, 0, 0.0)
                        points.add(mapOf("ts" to ts, "price" to price))
                    }
                }
                result[date.toString()] = points
                base = points.last()["price"] as Double
            }
            stocksCol.document(doc.id).update("lastWeekHistory", result).await()
        }
    }

    private fun calcNextPrice(
        symbol: String,
        hour: Int,
        current: Double,
        buyVolume: Long,
        sellVolume: Long,
        momentum: Double
    ): Double {
        val vol = when (symbol.uppercase()) {
            "MESS" -> if (hour in 8..9 || hour in 12..13 || hour in 19..21) 1.00 else 0.25
            "SFC"  -> if (hour in 11..14 || hour in 17..20) 0.80 else 0.20
            "MCFE" -> if (hour in 11..14 || hour in 17..20) 0.70 else 0.20
            "IIITS"-> if (hour in 18..22) 0.60 else 0.15
            "SMRKT"-> if (hour in 18..21) 0.45 else 0.12
            else   -> 0.25
        }
        val noise = Random.nextDouble(-vol, vol)
        val demandImpact = (buyVolume - sellVolume) * 0.001
        val socialImpact = momentum.coerceIn(-5.0, 5.0) * 0.02 // scale
        val next = maxOf(1.0, current + noise + demandImpact + socialImpact)
        return round(next * 100.0) / 100.0
    }


    // In StocksRepository.kt

    suspend fun onStockComment(stockId: String, sentiment: Int = 1) {
        // sentiment: +1 default; you can pass -1 for negative
        stocksCol.document(stockId).update("socialMomentum", FieldValue.increment(0.5 * sentiment)).await()
    }

    suspend fun onStockLike(stockId: String) {
        stocksCol.document(stockId).update("socialMomentum", FieldValue.increment(0.2)).await()
    }

    suspend fun onStockDislike(stockId: String) {
        stocksCol.document(stockId).update("socialMomentum", FieldValue.increment(-0.2)).await()
    }


    data class PortfolioLine(
        val symbol: String,
        val name: String,
        val qty: Long,
        val avgPrice: Double,
        val currentPrice: Double,
        val invested: Double,
        val currentValue: Double,
        val pnl: Double
    )

    suspend fun seedLastWeekAndDistributeToUsers(userUids: List<String>) {
        require(userUids.size == 5) { "Provide exactly 5 user UIDs" }

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val stocksSnap = db.collection("stocks").get().await()

        for (stockDoc in stocksSnap.documents) {
            val stockId = stockDoc.id
            val symbol = stockDoc.getString("symbol") ?: continue
            val name = stockDoc.getString("name") ?: symbol

            val totalSupply = (stockDoc.getLong("totalSupply") ?: 0L)
            val currentAvailable = (stockDoc.getLong("availableSupply") ?: totalSupply)

            // Start from current price if present, else a sane default
            var base = stockDoc.getDouble("price") ?: 10.0

            // Build a new independent map for THIS stock
            val lwhForStock = mutableMapOf<String, List<Map<String, Any>>>()

            // Generate last 6 full days (keep today empty; runtime engine will fill today)
            for (d in 6 downTo 1) {
                val date = today.minusDays(d.toLong())
                val points = mutableListOf<Map<String, Any>>()
                var price = base
                for (h in 5..22) {
                    for (m in listOf(0, 15, 30, 45)) {
                        price = calcSeedPrice(symbol, h, price)
                        val ts = date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
                        points.add(mapOf("ts" to ts, "price" to price))
                    }
                }
                lwhForStock[date.toString()] = points.toList() // copy
                base = points.last()["price"] as Double
            }

            // Write lastWeekHistory and reset today's history for THIS stock
            val stockRef = db.collection("stocks").document(stockId)
            stockRef.update(
                mapOf(
                    "lastWeekHistory" to lwhForStock,
                    "priceHistory" to emptyList<Map<String, Any>>(),
                    "price" to base,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            // Distribute available supply to 5 users
            if (currentAvailable > 0) {
                val parts = splitSupplyAcrossFive(currentAvailable)

                val batch = db.batch()
                var investorsAdded = 0L
                parts.forEachIndexed { idx, qty ->
                    if (qty <= 0L) return@forEachIndexed
                    val uid = userUids[idx]
                    val userRef = db.collection("users").document(uid)
                    val holdingRef = userRef.collection("holdings").document(stockId)

                    val avgSeedPrice = base // simple choice; or average of last day points

                    batch.set(
                        holdingRef,
                        mapOf(
                            "stockId" to stockId,
                            "qty" to qty,
                            "avgPrice" to avgSeedPrice
                        )
                    )
                    investorsAdded += 1
                }

                val consumed = parts.sum()
                batch.update(stockRef, "availableSupply", (currentAvailable - consumed).coerceAtLeast(0L))
                if (investorsAdded > 0) {
                    batch.update(stockRef, "investorsCount", FieldValue.increment(investorsAdded))
                }
                batch.commit().await()
            }
        }
    }
    private fun splitSupplyAcrossFive(available: Long): List<Long> {
        if (available <= 0L) return listOf(0,0,0,0,0)
        val weights = List(5) { Random.nextDouble(0.5, 1.5) }
        val sum = weights.sum()
        val raw = weights.map { (it / sum) * available }
        val rounded = raw.map { it.toLong() }.toMutableList()
        var diff = available - rounded.sum()
        var i = 0
        while (diff > 0) { rounded[i % 5] += 1; diff--; i++ }
        return rounded
    }

    private fun calcSeedPrice(symbol: String, hour: Int, current: Double): Double {
        val vol = when (symbol.uppercase()) {
            "MESS" -> if (hour in 8..9 || hour in 12..13 || hour in 19..21) 1.00 else 0.25
            "SFC", "MCFE" -> if (hour in 11..14 || hour in 17..20) 0.70 else 0.20
            "IIITS"-> if (hour in 18..22) 0.60 else 0.15
            "SMRKT"-> if (hour in 18..21) 0.45 else 0.12
            else   -> 0.25
        }
        val noise = Random.nextDouble(-vol, vol)
        val drift = when (symbol.uppercase()) {
            "MESS" -> if (hour in 12..13) 0.10 else 0.0
            "SFC", "MCFE" -> if (hour in 18..20) 0.08 else 0.0
            else -> 0.0
        }
        val next = max(1.0, current + noise + drift)
        return round(next * 100.0) / 100.0
    }

    // Read a user's portfolio lines with P/L
    suspend fun getUserPortfolio(uid: String): List<PortfolioLine> {
        val userRef = db.collection("users").document(uid)
        val holdings = userRef.collection("holdings").get().await().documents
        val lines = mutableListOf<PortfolioLine>()
        for (h in holdings) {
            val stockId = h.getString("stockId") ?: continue
            val qty = h.getLong("qty") ?: 0L
            val avgPrice = h.getDouble("avgPrice") ?: 0.0
            val stock = db.collection("stocks").document(stockId).get().await()
            val symbol = stock.getString("symbol") ?: stockId
            val name = stock.getString("name") ?: symbol
            val currentPrice = stock.getDouble("price") ?: avgPrice
            val invested = avgPrice * qty
            val currentValue = currentPrice * qty
            val pnl = currentValue - invested
            lines.add(
                PortfolioLine(symbol, name, qty, avgPrice, currentPrice, invested, currentValue, pnl)
            )
        }
        return lines.sortedByDescending { it.currentValue }
    }


}