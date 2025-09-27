package com.nc.bangingbulls.stocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nc.bangingbulls.Home.Stocks.Comment
import com.nc.bangingbulls.Home.Stocks.Stock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch




class StocksViewModel(
    private val repo: StocksRepository = StocksRepository()
) : ViewModel() {

    val stocks: StateFlow<List<Stock>> = repo.observeStocks()
        .map { it.sortedByDescending { s -> s.investorsCount } }
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

    fun buy(stockId: String, qty: Long, maxTotal: Double, onError: (String) -> Unit = {}) = viewModelScope.launch {
        try {
            repo.buyStock(stockId, qty, maxTotal)
        } catch (e: Exception) {
            onError(e.message ?: "buy failed")
        }
    }

    fun sell(stockId: String, qty: Long, minAcceptable: Double, onError: (String) -> Unit = {}) = viewModelScope.launch {
        try {
            repo.sellStock(stockId, qty, minAcceptable)
        } catch (e: Exception) {
            onError(e.message ?: "sell failed")
        }
    }

    fun addComment(stockId: String, comment: Comment) = viewModelScope.launch {
        repo.addComment(stockId, comment)
    }





}
