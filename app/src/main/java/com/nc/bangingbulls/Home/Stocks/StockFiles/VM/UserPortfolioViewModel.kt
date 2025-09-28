package com.nc.bangingbulls.Home.Stocks.StockFiles.VM

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.PortfolioLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserPortfolioViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {
    private val _coins = MutableStateFlow(0.0)
    val coins: StateFlow<Double> = _coins

    private val _lines = MutableStateFlow<List<PortfolioLine>>(emptyList())
    val lines: StateFlow<List<PortfolioLine>> = _lines

    val netWorth: StateFlow<Double> = combine(_coins, _lines) { c, l -> c + l.sumOf { it.value } }
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), 0.0)

    fun start(uid: String) {
        db.collection("users").document(uid).addSnapshotListener { d, _ ->
            val c = d?.getDouble("coins") ?: d?.getLong("coins")?.toDouble() ?: 0.0
            _coins.value = c
        }
        db.collection("users").document(uid).collection("holdings")
            .addSnapshotListener { snap, _ ->
                viewModelScope.launch {
                    val docs = snap?.documents ?: emptyList()
                    val prices = db.collection("stocks").get().await().documents.associateBy({ it.id }) {
                        Triple(
                            it.getString("symbol") ?: it.id,
                            it.getString("name") ?: (it.getString("symbol") ?: it.id),
                            it.getDouble("price") ?: 0.0
                        )
                    }
                    val list = docs.mapNotNull { h ->
                        val stockId = h.getString("stockId") ?: return@mapNotNull null
                        val qty = h.getLong("qty") ?: 0L
                        val avg = h.getDouble("avgPrice") ?: 0.0
                        val triple = prices[stockId] ?: Triple(stockId, stockId, 0.0)
                        val symbol = triple.first
                        val name = triple.second
                        val current = triple.third
                        PortfolioLine(stockId, symbol, name, qty, avg, current, current * qty)
                    }.sortedByDescending { it.value }
                    _lines.value = list
                }
            }
    }
}