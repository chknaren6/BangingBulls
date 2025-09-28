package com.nc.bangingbulls.Home.Stocks.Leaderboard.VM

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LeaderboardRow(
    val uid: String = "",
    val username: String = "",
    val coins: Double = 0.0,
    val portfolio: Double = 0.0,
    val totalCoins: Double = 0.0
)

class LeaderboardViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {
    private val _rows = MutableStateFlow<List<LeaderboardRow>>(emptyList())
    val rows: StateFlow<List<LeaderboardRow>> = _rows

    init {
        db.collection("leaderboard")
            .orderBy("totalCoins", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { d ->
                    LeaderboardRow(
                        uid = d.getString("uid") ?: "",
                        username = d.getString("username") ?: "",
                        coins = d.getDouble("coins") ?: 0.0,
                        portfolio = d.getDouble("portfolio") ?: 0.0,
                        totalCoins = d.getDouble("totalCoins") ?: 0.0
                    )
                } ?: emptyList()
                _rows.value = list
            }
    }
}