package com.nc.bangingbulls.Home.Stocks.StockFiles

import android.annotation.SuppressLint
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

@SuppressLint("StaticFieldLeak")
val db = FirebaseFirestore.getInstance()
val stocksCol = db.collection("stocks")

fun updateStockDataModel(stockId: String) {
    val updates = hashMapOf<String, Any>(
        "priceHistory" to listOf<Map<String, Any>>(), // Today's timestamped price points (will reset daily)
        "lastWeekHistory" to hashMapOf<String, List<Map<String, Any>>>(), // Last 7 days (dateString -> list of points)
        "lastUpdated" to Timestamp.now()
    )
    stocksCol.document(stockId).update(updates)
        .addOnSuccessListener { println("Updated $stockId: priceHistory and lastWeekHistory added!") }
        .addOnFailureListener { err -> println("Error updating $stockId: ${err.message}") }
}

