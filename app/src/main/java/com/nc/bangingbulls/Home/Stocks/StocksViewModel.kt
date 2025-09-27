package com.nc.bangingbulls.stocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nc.bangingbulls.Home.Stocks.Comments.Comment
import com.nc.bangingbulls.Home.Stocks.Stock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class StocksViewModel(
    private val repo: StocksRepository = StocksRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val stocks: StateFlow<List<Stock>> =
        repo.observeStocks().map { it.sortedByDescending { s -> s.investorsCount } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun like(stockId: String) = viewModelScope.launch {
        repo.likeStock(stockId)
    }

    fun dislike(stockId: String) = viewModelScope.launch {
        repo.dislikeStock(stockId)
    }

    fun buy(stockId: String, qty: Long, maxTotal: Double, onError: (String) -> Unit = {}) =
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

    fun addComment(stockId: String, comment: Comment) = viewModelScope.launch {
        repo.addComment(stockId, comment)
    }


    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private var replyTo: Comment? = null

    fun loadComments(stockId: String) {
        db.collection("stocks").document(stockId).collection("comments")
            .orderBy("ts", Query.Direction.ASCENDING).addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    Comment.fromMap(doc.id, doc.data ?: emptyMap())
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
                val parentComment = Comment.fromMap(snapshot.id, parentData)
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
    }


    fun likeComment(stockId: String, commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        val commentRef =
            db.collection("stocks").document(stockId).collection("comments").document(commentId)

        commentRef.get().addOnSuccessListener { snapshot ->
            val comment = snapshot.data?.let { Comment.fromMap(snapshot.id, it) }
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
            val comment = snapshot.data?.let { Comment.fromMap(snapshot.id, it) }
                ?: return@addOnSuccessListener
            val newDislikes =
                if (uid !in comment.dislikes) comment.dislikes + uid else comment.dislikes
            val newLikes = comment.likes - uid
            commentRef.update(mapOf("likes" to newLikes, "dislikes" to newDislikes))
        }
    }


    fun prepareReply(comment: Comment) {
        replyTo = comment
    }


}
