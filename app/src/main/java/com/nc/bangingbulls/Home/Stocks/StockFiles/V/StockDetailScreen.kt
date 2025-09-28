package com.nc.bangingbulls.Home.Stocks.StockFiles.V

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.HoldingAdminControls
import com.nc.bangingbulls.Home.Stocks.Comments.M.Comment
import com.nc.bangingbulls.Home.Stocks.Comments.V.CommentInput
import com.nc.bangingbulls.Home.Stocks.Comments.V.CommentItem
import com.nc.bangingbulls.Home.Stocks.StockFiles.VM.StocksViewModel
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

@Composable
fun StockDetailScreen(
    stockId: String,
    navController: NavController,
    stocksViewModel: StocksViewModel,
    userViewModel: UserViewModel
) {
    val stockState by stocksViewModel.observeStock(stockId).collectAsState(initial = null)
    val stock = stockState ?: return Column { Text("Loading...") }

    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val holdingState = remember(stockId, uid) { mutableStateOf<Long?>(null) }
    var newComment by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("anonymous") }
    val isAdmin = remember { mutableStateOf(false) }
    LaunchedEffect(stockId, uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("holdings")
            .document(stockId)
            .addSnapshotListener { d, _ -> holdingState.value = d?.getLong("qty") }
    }
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { d -> isAdmin.value = d.getBoolean("admin") == true }
    }

    val commentsFlow = remember(stockId) {
        callbackFlow<List<Comment>> {
            val sub = FirebaseFirestore.getInstance().collection("stocks").document(stockId)
                .collection("comments").orderBy("ts").addSnapshotListener { snap, err ->
                    if (err != null) {
                        close(err); return@addSnapshotListener
                    }
                    val list = snap?.documents?.map {
                        Comment(
                            id = it.id,
                            userId = it.getString("authorUid") ?: "",
                            username = it.getString("authorName")?.lowercase() ?: "anonymous",
                            text = it.getString("text") ?: "",
                            ts = it.getLong("ts") ?: 0L
                        )
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { sub.remove() }
        }
    }
    val comments by commentsFlow.collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.stocksbg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xCC1E1E1E), shape = RoundedCornerShape(12.dp)
                    ) // darker semi-transparent + rounded corners
                    .padding(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stock.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(stock.symbol, fontSize = 16.sp, color = Color.White)
                        Text(
                            "Holding: ${holdingState.value ?: 0L}",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        "Coins: 💰${userViewModel.coins}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Yellow
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chart
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        val prices = mergedLifetimePoints(stock).map { it.price }
                        if (prices.isNotEmpty()) SimpleLineChart(prices = prices)
                        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No data available", color = Color.Gray)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(
                            onClick = { stocksViewModel.like(stock.id) },
                            modifier = Modifier.background(Color.Black, RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.thumb_up),
                                contentDescription = "Like",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { stocksViewModel.dislike(stock.id) },
                            modifier = Modifier.background(Color.Black, RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.thumb_down),
                                contentDescription = "Dislike",
                                tint = Color.White
                            )
                        }
                    }
                }
                item { BuySellHalfWidth(stock.id, stock.price, stocksViewModel) }
                if (isAdmin.value) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Admin Holdings Controls",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            HoldingAdminControls(uid = uid, stockId = stockId)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { /* delete */ },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                ) {
                                    Text("Delete", color = Color.White)
                                }
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), shape = RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "Comments",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(12.dp))
                        CommentInput(
                            text = newComment,
                            onTextChange = { newComment = it },
                            onSend = {
                                if (newComment.isNotBlank()) {
                                    stocksViewModel.addComment(stockId, newComment, null, username)
                                    newComment = ""
                                }
                            })

                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                comments.forEach { comment ->
                                    CommentItem(
                                        comment = comment,
                                        onLike = { stocksViewModel.likeComment(stockId, it.id) },
                                        onDislike = {
                                            stocksViewModel.dislikeComment(
                                                stockId,
                                                it.id
                                            )
                                        },
                                        onReply = {})
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BuySellHalfWidth(stockId: String, price: Double, stocksViewModel: StocksViewModel) {
    var qtyText by remember { mutableStateOf("1") }
    val qty = qtyText.toLongOrNull() ?: 0L
    val total = price * qty
    var confirm by remember { mutableStateOf<String?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = qtyText,
            onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Qty", color = Color.Black) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.5f)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(0.5f)) {
            Text("Net: ${"%,.2f".format(total)}", fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (qty > 0) confirm = "buy" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) { Text("Buy", color = Color.White) }
                Button(
                    onClick = { if (qty > 0) confirm = "sell" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) { Text("Sell", color = Color.White) }
            }
        }
    }

    confirm?.let { type ->
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (type == "buy") "Confirm Buy" else "Confirm Sell") },
            text = { Text("${type.uppercase()} $qty for ~ ${"%,.2f".format(price * qty)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val callback: (Boolean, String?) -> Unit = { ok, err ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (ok) "$type success"
                                    else "$type failed: ${err ?: "unknown error"}"
                                )
                            }
                        }
                        if (type == "buy") stocksViewModel.buy(stockId, qty, callback)
                        else stocksViewModel.sell(stockId, qty, callback)
                        confirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) { Text("Confirm", color = Color.White) }
            },
            dismissButton = {
                Button(
                    onClick = { confirm = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) { Text("Cancel", color = Color.White) }
            })
    }
}