// StockDetailScreen.kt
package com.nc.bangingbulls.Home.Stocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.HoldingAdminControls
import com.nc.bangingbulls.Home.Stocks.Comments.Comment
import com.nc.bangingbulls.Home.Stocks.Comments.CommentInput
import com.nc.bangingbulls.Home.Stocks.Comments.CommentsList
import com.nc.bangingbulls.stocks.StocksViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

@Composable
fun StockDetailScreen(
    stockId: String,
    navController: NavController,
    stocksViewModel: StocksViewModel,
) {
    val stockState by stocksViewModel.observeStock(stockId).collectAsState(initial = null)
    val stock = stockState ?: return Column { Text("Loading...") }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val holdingState = remember(stockId, uid) { mutableStateOf<Long?>(null) }
    var newComment by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("Anonymous") }
    val isAdmin = remember { mutableStateOf(false) }

    // user holdings
    LaunchedEffect(stockId, uid) {
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("holdings").document(stockId)
            .addSnapshotListener { d, _ ->
                holdingState.value = d?.getLong("qty")
            }
    }

    // admin flag
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { d -> isAdmin.value = d.getBoolean("admin") == true }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stock.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(stock.symbol)
                Text("Holding: ${holdingState.value ?: 0L}")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Price: ${"%,.2f".format(stock.price)}")
                Text("Supply: ${stock.availableSupply}/${stock.totalSupply}")
            }
        }

        Spacer(Modifier.height(12.dp))
        SimpleLineChart(points = mergedLifetimePoints(stock).map { it.price })
        Spacer(Modifier.height(8.dp))

        BuySellRow(
            stockId = stock.id,
            price = stock.price,
            stocksViewModel = stocksViewModel
        )

        Spacer(Modifier.height(8.dp))

        Row {
            Button(onClick = { stocksViewModel.like(stock.id) }) { Text("Like") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { stocksViewModel.dislike(stock.id) }) { Text("Dislike") }
        }

        if (isAdmin.value) {
            Spacer(Modifier.height(16.dp))
            Text("Admin Holdings Controls", fontWeight = FontWeight.Bold)
            HoldingAdminControls(uid = uid, stockId = stockId)
        }

        Spacer(Modifier.height(12.dp))

        // Comments section
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
                                userId = it.getString("authorUid") ?: "",
                                username = it.getString("authorName") ?: "",
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
        var replyToComment by remember { mutableStateOf<Comment?>(null) }

        Text("Comments", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            CommentInput(
                text = newComment,
                onTextChange = { newComment = it },
                onSend = {
                    if (newComment.isNotBlank()) {
                        stocksViewModel.addComment(
                            stockId = stockId,
                            text = newComment,
                            replyToId = replyToComment?.id,
                            username = username
                        )
                        newComment = ""
                        replyToComment = null
                    }
                },
                replyingTo = replyToComment
            )

            Spacer(Modifier.height(8.dp))
            CommentsList(
                comments = comments,
                onLike = { stocksViewModel.likeComment(stockId, it.id) },
                onDislike = { stocksViewModel.dislikeComment(stockId, it.id) },
                onReply = { replyToComment = it }
            )
        }
    }
}


@Composable
fun BuySellRow(
    stockId: String,
    price: Double,
    stocksViewModel: StocksViewModel
) {
    var qtyText by remember { mutableStateOf("1") }
    val qty = qtyText.toLongOrNull() ?: 0L
    val total = price * qty

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Place a SnackbarHost somewhere in parent if you want global
    SnackbarHost(hostState = snackbarHostState)

    var confirm by remember { mutableStateOf<String?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = qtyText,
            onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Qty") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.width(140.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text("Total: ${"%,.2f".format(total)}")
        Spacer(Modifier.width(12.dp))
        Button(onClick = { if (qty > 0) confirm = "buy" }) { Text("Buy") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = { if (qty > 0) confirm = "sell" }) { Text("Sell") }
    }

    confirm?.let { type ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (type == "buy") "Confirm Buy" else "Confirm Sell") },
            text = { Text("${type.uppercase()} $qty for ~ ${"%,.2f".format(price * qty)}?") },
            confirmButton = {
                Button(onClick = {
                    val callback: (Boolean, String?) -> Unit = { ok, err ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (ok) "$type success"
                                else "$type failed: ${err ?: "unknown error"}"
                            )
                        }
                    }
                    if (type == "buy") {
                        stocksViewModel.buy(stockId, qty, callback)
                    } else {
                        stocksViewModel.sell(stockId, qty, callback)
                    }
                    confirm = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                Button(onClick = { confirm = null }) { Text("Cancel") }
            }
        )
    }
}
