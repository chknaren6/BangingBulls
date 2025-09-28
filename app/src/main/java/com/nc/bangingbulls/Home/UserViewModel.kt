package com.nc.bangingbulls.Home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.Holding

class UserViewModel : ViewModel() {
    private val db = Firebase.firestore
    val auth = Firebase.auth

    var isAdmin by mutableStateOf(false)
    var username by mutableStateOf("")
    var coins: Long by mutableStateOf(0)
    var profileUrl by mutableStateOf<String?>(null)
    var holdings by mutableStateOf<List<Holding>>(emptyList())
    var leaderboard by mutableStateOf<List<LeaderboardUser>>(emptyList())

    private val userRef
        get() = auth.currentUser?.uid?.let { db.collection("users").document(it) }

    init {
        loadUserData()
        loadHoldings()
        loadLeaderboard()
    }

    fun loadUserData() {
        val ref = userRef ?: return
        ref.addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                username = snap.getString("username") ?: ""
                coins = (snap.getLong("coins") ?: 0) as Long
                profileUrl = snap.getString("profileUrl")
                isAdmin = snap.getBoolean("admin") ?: false
            }
        }
    }

    fun loadHoldings() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("holdings")
            .addSnapshotListener { snap, _ ->
                holdings = snap?.documents?.mapNotNull { doc ->
                    val sid = doc.getString("stockId") ?: return@mapNotNull null
                    val qty = doc.getLong("qty") ?: 0L
                    val avg = doc.getDouble("avgPrice") ?: 0.0
                    Holding(stockId = sid, qty = qty, avgPrice = avg)
                } ?: emptyList()

            }
    }

    fun loadLeaderboard() {
        db.collection("leaderboard")
            .orderBy("totalCoins", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snap, _ ->
                leaderboard = snap?.documents?.map { doc ->
                    LeaderboardUser(
                        uid = doc.getString("uid") ?: "",
                        username = doc.getString("username") ?: "",
                        totalCoins = doc.getDouble("totalCoins")?.toInt() ?: 0
                    )
                } ?: emptyList()
            }
    }

    fun addCoins(amount: Long) {
        val uid = auth.currentUser?.uid ?: return
            coins += amount  // update local state
            db.collection("users").document(uid)
                .update("coins", FieldValue.increment(amount.toLong())) // update Firestore

    }

    fun deductCoins(amount: Long) {
        val uid = auth.currentUser?.uid ?: return
        if (coins >= amount) {
            coins -= amount  // update local state
            db.collection("users").document(uid)
                .update("coins", FieldValue.increment(-amount.toLong())) // update Firestore
        }
    }
    fun updateCoins(amount: Long) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)

        // Optimistically update local state
        coins += amount

        // Firestore atomic increment
        ref.update("coins", FieldValue.increment(amount))
            .addOnSuccessListener {
                Log.d("UserViewModel", "Coins updated in Firestore by $amount")
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Failed to update coins: ${e.message}")
            }
    }



    private fun saveCoinsToDB(newCoins: Long) {
        val userId = auth.currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("coins", newCoins)
            .addOnSuccessListener { Log.d("UserViewModel", "Coins updated to $newCoins") }
            .addOnFailureListener { e -> Log.e("UserViewModel", "Failed to update coins", e) }
    }

    fun claimDailyCoins() {
        val ref = userRef ?: return
        ref.get().addOnSuccessListener { snap ->
            val lastTs = snap.getLong("lastRewardTimestamp") ?: 0L
            val now = System.currentTimeMillis()
            if (now - lastTs >= 24 * 3600 * 1000L) {
                ref.update(
                    mapOf(
                        "coins" to FieldValue.increment(2675),
                        "lastRewardTimestamp" to now
                    )
                )
                coins += 2675
            }
        }
    }

    fun updateUsername(newUsername: String) {
        val ref = userRef ?: return
        ref.update("username", newUsername)
        username = newUsername
    }
}
