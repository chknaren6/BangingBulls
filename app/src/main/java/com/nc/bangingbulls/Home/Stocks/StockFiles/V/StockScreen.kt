package com.nc.bangingbulls.Home.Stocks.StockFiles.V

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.Stocks.StockFiles.VM.StocksViewModel
import com.nc.bangingbulls.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun StocksScreen(navController: NavController, stocksViewModel: StocksViewModel) {
    val stocks by stocksViewModel.stocks.collectAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val config = LocalConfiguration.current
    val isNarrow = config.screenWidthDp <= 360

    val nameSize = if (isNarrow) 14.sp else 15.sp
    val symbolSize = if (isNarrow) 11.sp else 12.sp
    val priceSize = if (isNarrow) 13.sp else 14.sp
    val miscSize = if (isNarrow) 11.sp else 12.sp

    val cardBg = listOf(
        Color(0xFF0E1320), Color(0xFF121A2C), Color(0xFF142036), Color(0xFF10182A)
    )
    val textPrimary = Color.White
    val textMuted = Color(0xFFB7C3DB)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.stockscreen),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x44000000),
                        0.6f to Color.Transparent,
                        1f to Color(0x66000000)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stocks.chunked(2)) { rowStocks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowStocks.forEachIndexed { index, stock ->
                        var myQty by remember(stock.id) { mutableStateOf(0L) }
                        LaunchedEffect(stock.id, uid) {
                            if (uid != null) {
                                FirebaseFirestore.getInstance()
                                    .collection("users").document(uid)
                                    .collection("holdings").document(stock.id)
                                    .addSnapshotListener { d, _ ->
                                        myQty = d?.getLong("qty") ?: 0L
                                    }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isNarrow) 160.dp else 172.dp)
                                .clickable { navController.navigate("stock/${stock.id}") },
                            colors = CardDefaults.cardColors(containerColor = cardBg[index % cardBg.size]),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = stock.name,
                                        color = textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = nameSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = stock.symbol.uppercase(),
                                        color = textMuted,
                                        fontSize = symbolSize,
                                        maxLines = 1
                                    )
                                }

                                Text(
                                    text = "₹ ${"%,.2f".format(stock.price)}",
                                    color = Color(0xFFBFD1FF),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = priceSize,
                                    maxLines = 1
                                )
                                Column {
                                    Text(
                                        text = "Holding: $myQty",
                                        color = textMuted,
                                        fontSize = miscSize,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Supply: ${stock.availableSupply}/${stock.totalSupply}",
                                        color = textMuted,
                                        fontSize = miscSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (rowStocks.size < 2) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
