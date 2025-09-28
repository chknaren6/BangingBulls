package com.nc.bangingbulls.Home.Stocks.StockFiles.VM

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nc.bangingbulls.Home.Stocks.Comments.M.Comment
import com.nc.bangingbulls.Home.Stocks.Leaderboard.M.LeaderboardRepository
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.Stock
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.StocksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.plus

class StocksViewModel(
    private val repo: StocksRepository = StocksRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val stocks: StateFlow<List<Stock>> =
        repo.observeStocks().map { it.sortedByDescending { s -> s.investorsCount } }
            .stateIn(viewModelScope, SharingStarted.Companion.Lazily, emptyList())

    fun observeStock(stockId: String) = repo.observeStock(stockId)

    fun addStock(stock: Stock) = viewModelScope.launch {
        repo.addStock(stock)
    }

    fun addStockAsAdmin(stock: Stock, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.addStock(stock)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add stock")
            }
        }
    }

   /* fun buy(stockId: String, qty: Long, maxTotal: Double, onError: (String) -> Unit = {}) =
        viewModelScope.launch {
            try {
                repo.buyStock(stockId, qty, maxTotal)
            } catch (e: Exception) {
                onError(e.message ?: "buy failed")
            }
        }

    fun sell(stockId: String, qty: Long, minAcceptable: Double, onError: (String) -> Unit = {}) =
        viewModelScope.launch {
            try {
                repo.sellStock(stockId, qty, minAcceptable)
            } catch (e: Exception) {
                onError(e.message ?: "sell failed")
            }
        }
*/


    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private var replyTo: Comment? = null

    fun loadComments(stockId: String) {
        db.collection("stocks").document(stockId).collection("comments")
            .orderBy("ts", Query.Direction.ASCENDING).addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    Comment.Companion.fromMap(doc.id, doc.data ?: emptyMap())
                }
                _comments.value = list
            }
    }


    fun addComment(stockId: String, text: String, replyToId: String? = null, username: String) {
        val user = auth.currentUser ?: return
        val commentRef = db.collection("stocks").document(stockId).collection("comments")

        val newCommentId = commentRef.document().id
        val newComment = hashMapOf(
            "id" to newCommentId,
            "userId" to user.uid,
            "username" to username,
            "text" to text,
            "ts" to System.currentTimeMillis(),
            "likes" to listOf<String>(),
            "dislikes" to listOf<String>(),
            "replies" to listOf<Map<String, Any>>()
        )

        if (replyToId == null) {
            // top level comment
            commentRef.document(newCommentId).set(newComment)
        } else {
            // reply to existing comment
            val parentRef = commentRef.document(replyToId)
            parentRef.get().addOnSuccessListener { snapshot ->
                val parentData = snapshot.data ?: return@addOnSuccessListener
                val parentComment = Comment.Companion.fromMap(snapshot.id, parentData)
                val updatedReplies = parentComment.replies + Comment(
                    id = newCommentId,
                    userId = user.uid,
                    username = username,
                    text = text,
                    likes = emptyList(),
                    dislikes = emptyList(),
                    replies = emptyList()
                )
                parentRef.update("replies", updatedReplies.map { reply ->
                    mapOf(
                        "id" to reply.id,
                        "userId" to reply.userId,
                        "username" to reply.username,
                        "text" to reply.text,
                        "likes" to reply.likes,
                        "dislikes" to reply.dislikes,
                        "replies" to reply.replies.map {
                            mapOf(
                                "id" to it.id,
                                "text" to it.text
                            )
                        }, // nested replies simplified
                        "ts" to reply.ts
                    )
                })
            }
        }
        viewModelScope.launch { repo.onStockComment(stockId, sentiment = 1) }

    }


    fun likeComment(stockId: String, commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        val commentRef =
            db.collection("stocks").document(stockId).collection("comments").document(commentId)

        commentRef.get().addOnSuccessListener { snapshot ->
            val comment = snapshot.data?.let { Comment.Companion.fromMap(snapshot.id, it) }
                ?: return@addOnSuccessListener
            val newLikes = if (uid !in comment.likes) comment.likes + uid else comment.likes
            val newDislikes = comment.dislikes - uid
            commentRef.update(mapOf("likes" to newLikes, "dislikes" to newDislikes))
        }
    }

    fun dislikeComment(stockId: String, commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        val commentRef =
            db.collection("stocks").document(stockId).collection("comments").document(commentId)

        commentRef.get().addOnSuccessListener { snapshot ->
            val comment = snapshot.data?.let { Comment.Companion.fromMap(snapshot.id, it) }
                ?: return@addOnSuccessListener
            val newDislikes =
                if (uid !in comment.dislikes) comment.dislikes + uid else comment.dislikes
            val newLikes = comment.likes - uid
            commentRef.update(mapOf("likes" to newLikes, "dislikes" to newDislikes))
        }
    }

    fun like(stockId: String) = viewModelScope.launch {
        repo.likeStock(stockId)
        repo.onStockLike(stockId)
    }

    fun dislike(stockId: String) = viewModelScope.launch {
        repo.dislikeStock(stockId)
        repo.onStockDislike(stockId)
    }

    fun prepareReply(comment: Comment) {
        replyTo = comment
    }

    // StocksViewModel.kt
    fun migrateAllStocksModel(onDone: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val stocks = db.collection("stocks").get().await()
                val batch = db.batch()
                stocks.documents.forEach { doc ->
                    val ref = doc.reference
                    batch.update(ref, mapOf(
                        "priceHistory" to listOf<Map<String, Any>>(),
                        "lastWeekHistory" to mapOf<String, List<Map<String, Any>>>(),
                        "lastUpdated" to Timestamp.now(),
                        "socialMomentum" to 0.0 // new field for interactions
                    ))
                }
                batch.commit().await()
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "migrate failed")
            }
        }
    }


    // StocksViewModel.kt (append)

    data class PortfolioUi(
        val symbol: String,
        val name: String,
        val qty: Long,
        val avgPrice: Double,
        val currentPrice: Double,
        val invested: Double,
        val currentValue: Double,
        val pnl: Double
    )

    private val _portfolio = MutableStateFlow<List<PortfolioUi>>(emptyList())
    val portfolio: StateFlow<List<PortfolioUi>> = _portfolio



    fun loadPortfolio(uid: String) {
        viewModelScope.launch {
            val lines = repo.getUserPortfolio(uid)
            _portfolio.value = lines.map {
                PortfolioUi(
                    it.symbol, it.name, it.qty, it.avgPrice, it.currentPrice,
                    it.invested, it.currentValue, it.pnl
                )
            }
        }
    }
    // StocksViewModel.kt

    fun buy(stockId: String, qty: Long, onResult: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            val res = repo.buyStockTransactional(stockId, qty)
            onResult(res.isSuccess, res.exceptionOrNull()?.message)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                viewModelScope.launch {
                    LeaderboardRepository().recomputeForCurrentUser()
                }
            }
        }

    fun sell(stockId: String, qty: Long, onResult: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            val res = repo.sellStockTransactional(stockId, qty)
            onResult(res.isSuccess, res.exceptionOrNull()?.message)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                viewModelScope.launch {
                    LeaderboardRepository().recomputeForCurrentUser()
                }
            }
        }

    suspend fun recomputeLeaderboardForCurrentUser(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val u = db.collection("users").document(uid).get().await()
        val coins = (u.getDouble("coins") ?: u.getLong("coins")?.toDouble() ?: 0.0)

        val holdings = db.collection("users").document(uid).collection("holdings").get().await().documents
        val prices = db.collection("stocks").get().await().documents.associateBy({ it.id }) {
            it.getDouble("price") ?: 0.0
        }

        var portfolioValue = 0.0
        for (h in holdings) {
            val sid = h.getString("stockId") ?: continue
            val qty = h.getLong("qty") ?: 0L
            portfolioValue += (prices[sid] ?: 0.0) * qty
        }

        val total = coins + portfolioValue
        val lbRef = db.collection("leaderboard").document(uid)
        lbRef.set(mapOf(
            "uid" to uid,
            "username" to (u.getString("username") ?: ""),
            "coins" to coins,
            "portfolio" to portfolioValue,
            "totalCoins" to total,
            "ts" to System.currentTimeMillis()
        )).await()
    }




}