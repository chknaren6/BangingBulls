package com.nc.bangingbulls.stocks

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.Stocks.Comment
import com.nc.bangingbulls.Home.Stocks.Stock
import com.nc.bangingbulls.Home.Stocks.toStock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
            "updatedAt" to Timestamp.now()
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
            "authorUid" to comment.authorUid,
            "authorName" to comment.authorName,
            "text" to comment.text,
            "ts" to comment.ts
        )
        doc.set(payload).await()
    }

    // Buy stock - transaction: debit user coins, add holding, update stock price and priceHistory
    suspend fun buyStock(stockId: String, qty: Long, maxTotalPayment: Double) {
        val auth = Firebase.auth
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No user")
        val userRef = usersCol.document(uid)
        val stockRef = stocksCol.document(stockId)
        db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val stockSnap = tx.get(stockRef)

            val userCoins = (userSnap.getLong("coins") ?: 0L).toDouble()
            val price = stockSnap.getDouble("price") ?: 0.0
            val totalCost = price * qty

            if (totalCost > maxTotalPayment) throw Exception("Price changed, abort")
            if (userCoins < totalCost) throw Exception("Insufficient coins")

            // debit coins
            tx.update(userRef, "coins", userCoins - totalCost)

            // update holdings (users/{uid}/holdings/{stockId})
            val holdingRef = userRef.collection("holdings").document(stockId)
            val holdingSnap = tx.get(holdingRef)
            if (holdingSnap.exists()) {
                val oldQty = holdingSnap.getLong("qty") ?: 0L
                val oldAvg = holdingSnap.getDouble("avgPrice") ?: 0.0
                val newQty = oldQty + qty
                val newAvg = ((oldAvg * oldQty) + (price * qty)) / (newQty)
                tx.update(holdingRef, mapOf("qty" to newQty, "avgPrice" to newAvg))
            } else {
                tx.set(holdingRef, mapOf("stockId" to stockId, "qty" to qty, "avgPrice" to price))
            }

            // update stock: availableSupply, investorsCount (crudely), and append price sample
            val available = (stockSnap.getLong("availableSupply") ?: stockSnap.getLong("totalSupply") ?: 0L) - qty
            tx.update(stockRef, "availableSupply", available)
            // update price a bit: newPrice = price * (1 + 0.001 * qty)
            val newPrice = price * (1 + 0.001 * qty)
            tx.update(stockRef, "price", newPrice)
            // append priceHistory small entry
            val point = mapOf("ts" to System.currentTimeMillis(), "price" to newPrice)
            tx.update(stockRef, "priceHistory", FieldValue.arrayUnion(point))

            // write a trade record
            val tradeDoc = tradesCol.document()
            tx.set(tradeDoc, mapOf(
                "uid" to uid,
                "stockId" to stockId,
                "qty" to qty,
                "price" to price,
                "type" to "buy",
                "ts" to System.currentTimeMillis()
            ))

            null
        }.await()
    }

    // Sell stock - transaction: credit user coins, reduce holding, update price
    suspend fun sellStock(stockId: String, qty: Long, minAcceptable: Double) {
        val auth = Firebase.auth
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No user")
        val userRef = usersCol.document(uid)
        val stockRef = stocksCol.document(stockId)

        db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val stockSnap = tx.get(stockRef)
            val holdingRef = userRef.collection("holdings").document(stockId)
            val holdingSnap = tx.get(holdingRef)

            val price = stockSnap.getDouble("price") ?: 0.0
            if (price < minAcceptable) throw Exception("Price below acceptable")

            val oldQty = holdingSnap.getLong("qty") ?: 0L
            if (oldQty < qty) throw Exception("Insufficient holding")

            val userCoins = (userSnap.getLong("coins") ?: 0L).toDouble()
            val totalRevenue = price * qty
            tx.update(userRef, "coins", userCoins + totalRevenue)

            val newQty = oldQty - qty
            if (newQty <= 0) tx.delete(holdingRef) else tx.update(holdingRef, "qty", newQty)

            // increase available supply
            val available = (stockSnap.getLong("availableSupply") ?: 0L) + qty
            tx.update(stockRef, "availableSupply", available)
            // price down a bit on sale
            val newPrice = price * (1 - 0.001 * qty)
            tx.update(stockRef, "price", newPrice)
            val point = mapOf("ts" to System.currentTimeMillis(), "price" to newPrice)
            tx.update(stockRef, "priceHistory", FieldValue.arrayUnion(point))

            val tradeDoc = tradesCol.document()
            tx.set(tradeDoc, mapOf(
                "uid" to uid,
                "stockId" to stockId,
                "qty" to qty,
                "price" to price,
                "type" to "sell",
                "ts" to System.currentTimeMillis()
            ))

            null
        }.await()
    }


}
