package com.nc.bangingbulls.Home.Stocks.StockFiles.V

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.Stocks.StockFiles.VM.StocksViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.nc.bangingbulls.R

@Composable
fun StocksScreen(navController: NavController, stocksViewModel: StocksViewModel) {
    val stocks by stocksViewModel.stocks.collectAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    val cardColors = listOf(
        Color(0xFFFFF3E0),
        Color(0xFFE3F2FD),
        Color(0xFFE8F5E9),
        Color(0xFFF3E5F5)
    )
    val textColor = Color(0xFF333333)

    Box (modifier = Modifier.fillMaxSize()){
        Image(
            painter = painterResource(id = R.drawable.stockscreen),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                .height(220.dp)
                                .clickable { navController.navigate("stock/${stock.id}") },
                            elevation = CardDefaults.cardElevation(6.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cardColors[index % cardColors.size])
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        stock.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = textColor
                                    )
                                    Text(
                                        stock.symbol,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Price: ${"%,.2f".format(stock.price)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Holding: $myQty",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                    Text(
                                        "Supply: ${stock.availableSupply}/${stock.totalSupply}",
                                        fontSize = 14.sp,
                                        color = textColor
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


