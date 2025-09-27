package com.nc.bangingbulls.Home.Stocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.stocks.StocksViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@Composable
fun StockDetailScreen(
    stockId: String,
    navController: NavController,
    stocksViewModel: StocksViewModel,
) {
    val stockState by stocksViewModel.observeStock(stockId).collectAsState(initial = null)
    var timeframe by remember { mutableStateOf("today") } // "today" or "lifetime"
    var qtyText by remember { mutableStateOf("1") }
    val stock = stockState ?: return Column { Text("Loading...") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stock.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(stock.symbol)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Price: ${"%,.2f".format(stock.price)}")
                Text("Likes: ${stock.likes}")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Chart
        val pricePoints = if (timeframe == "today") {
            //last N points
            stock.priceHistory.takeLast(30)
        } else {
            stock.priceHistory
        }
        SimpleLineChart(points = pricePoints.map { it.price })

        // timeframe toggle
        Row {
            Button(onClick = { timeframe = "today" }) { Text("Today") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { timeframe = "lifetime" }) { Text("Lifetime") }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = qtyText, onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } }, modifier = Modifier.width(120.dp), label = { Text("Qty") })
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val qty = qtyText.toLongOrNull() ?: 0L
                val total = stock.price * qty
                stocksViewModel.buy(stock.id, qty, total) { err -> /* show toast */ }
                // after buy, prompt comment: open dialog to enter comment -> vm.addComment(...)
            }) { Text("Buy") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val qty = qtyText.toLongOrNull() ?: 0L
                stocksViewModel.sell(stock.id, qty, stock.price) { err -> /* show toast */ }
                // prompt comment similarly
            }) { Text("Sell") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { stocksViewModel.like(stock.id) }) { Text("Like") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { stocksViewModel.dislike(stock.id) }) { Text("Dislike") }
        }

        Spacer(Modifier.height(12.dp))

        // Comments preview (simple read)
        val commentsFlow = remember(stockId) {
            callbackFlow<List<Comment>> {
                val sub = FirebaseFirestore.getInstance()
                    .collection("stocks").document(stockId)
                    .collection("comments")
                    .orderBy("ts")
                    .addSnapshotListener { snap, err ->
                        if (err != null) { close(err); return@addSnapshotListener }
                        val list = snap?.documents?.map {
                            Comment(
                                id = it.id,
                                authorUid = it.getString("authorUid") ?: "",
                                authorName = it.getString("authorName") ?: "",
                                text = it.getString("text") ?: "",
                                ts = (it.getLong("ts") ?: 0L)
                            )
                        } ?: emptyList()
                        trySend(list)
                    }
                awaitClose { sub.remove() }
            }
        }
        val comments by commentsFlow.collectAsState(initial = emptyList())

        Text("Comments", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(comments) { c ->
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(c.authorName, fontWeight = FontWeight.Bold)
                    Text(c.text)
                }
            }
        }
    }
}
