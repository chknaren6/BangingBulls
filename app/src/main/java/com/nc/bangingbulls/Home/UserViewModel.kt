package com.nc.bangingbulls.Home

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.nc.bangingbulls.Home.Stocks.Holding
import com.nc.bangingbulls.Home.User
class UserViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    var isAdmin =false
    var username by mutableStateOf("")
    var coins by mutableStateOf(0)
    var profileUrl by mutableStateOf<String?>(null)
    var holdings by mutableStateOf<List<Holding>>(emptyList())
    var leaderboard by mutableStateOf<List<LeaderboardUser>>(emptyList())

    fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    username = snapshot.getString("username") ?: ""
                    coins = snapshot.getLong("coins")?.toInt() ?: 0
                    profileUrl = snapshot.getString("profileUrl")
                    isAdmin = snapshot.getBoolean("admin")?:false
                }
            }
    }

    fun loadHoldings() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("holdings")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    holdings = snap.documents.map { doc ->
                        Holding(
                            stockId = doc.getString("stockId") ?: "",
                            qty = doc.getLong("qty") ?: 0L,
                            avgPrice = doc.getDouble("avgPrice") ?: 0.0
                        )
                    }
                }
            }
    }

    fun loadLeaderboard() {
        db.collection("leaderboard")
            .orderBy("totalCoins", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    leaderboard = snap.documents.map { doc ->
                        LeaderboardUser(
                            uid = doc.getString("uid") ?: "",
                            username = doc.getString("username") ?: "",
                            totalCoins = doc.getDouble("totalCoins")?.toInt() ?: 0
                        )
                    }
                }
            }
    }

    fun updateCoins(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("coins", FieldValue.increment(amount.toLong()))
    }

    fun claimDailyCoins() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(uid)
        userRef.get().addOnSuccessListener { snap ->
            val lastTs = snap.getLong("lastRewardTimestamp") ?: 0L
            val now = System.currentTimeMillis()
            if (now - lastTs >= 24 * 3600 * 1000L) {
                userRef.update(
                    "coins", FieldValue.increment(2675),
                    "lastRewardTimestamp", now
                )
            }
        }
    }

    fun updateUsername(newUsername: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("username", newUsername)
    }
}



