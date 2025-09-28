package com.nc.bangingbulls.Home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nc.bangingbulls.Authentication.VM.AuthViewModel
import com.nc.bangingbulls.Home.Stocks.Leaderboard.VM.LeaderboardViewModel
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.StocksRepository
import com.nc.bangingbulls.Home.Stocks.StockFiles.VM.StocksViewModel
import com.nc.bangingbulls.R
import kotlin.text.lowercase

@Composable
fun HomeScreenContent(
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel?,
    navController: NavController,
    navControllerHome: NavController,
    stocksViewModel: StocksViewModel,
    stocksRepository: StocksRepository,
    userId: String?
)
{
    val coins = (userViewModel.coins as? Long) ?: (userViewModel.coins as? Int)?.toLong() ?: 0L
    val animatedCoins by animateFloatAsState(
        targetValue = coins.toFloat(),
        animationSpec = tween(600),
        label = "animatedCoins"
    )

    val holdings by remember { derivedStateOf { userViewModel.holdings } }
    val stocks by remember { derivedStateOf { stocksViewModel.stocks.value } }

    val leaderboardViewModel = viewModel<LeaderboardViewModel>()
    val leaderboard by leaderboardViewModel.rows.collectAsState()

    val portfolio by produceState(
        initialValue = emptyList<StocksRepository.PortfolioLine>(),
        key1 = userId
    ) {
        value = if (userId.isNullOrBlank()) {
            emptyList()
        } else {
            stocksRepository.getUserPortfolio(userId)
        }
    }


    val titleSize = 12.sp
    val cellSize = 11.sp
    val cellSizeSmall = 10.sp // fallback for very narrow screens
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isNarrow = configuration.screenWidthDp <= 360
    val fCell = if (isNarrow) cellSizeSmall else cellSize


    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.homebg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar: user icon (left) and coins (right)
           /* Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.height(40.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Coins : 💰 ${animatedCoins.toInt()}", fontWeight = FontWeight.Bold)
                    }
                }
            }*/

            Spacer(Modifier.height(24.dp))

            Text("Leaderboard", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBFD1FF))
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(leaderboard) { index, row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1320)),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        when (index) {
                                            0 -> Color(0xFFFFD54F) // gold
                                            1 -> Color(0xFFB0BEC5) // silver
                                            2 -> Color(0xFFBCAAA4) // bronze
                                            else -> Color(0xFF3B68F7)
                                        },
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0E1320)
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = row.username.trim(),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Portfolio: ${row.portfolio.format(2)}   •   Coins: ${row.coins.format(2)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8FA3BF),
                                    maxLines = 1
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Surface(
                                color = Color(0xFF17203A),
                                shape = RoundedCornerShape(20.dp),
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    text = "Total ${row.totalCoins.format(2)}",
                                    color = Color(0xFFB6C7FF),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }


            /* Spacer(Modifier.height(24.dp))

             // Popular Stocks row
             Text("Popular Stocks", fontSize = 20.sp, fontWeight = FontWeight.Medium)
             Spacer(Modifier.height(8.dp))
             LazyRow(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.spacedBy(12.dp)
             ) {
                 items(stocks) { stock ->
                     val prevPrice = stock.priceHistory.lastOrNull()?.price ?: stock.price
                     Card(
                         modifier = Modifier
                             .padding(4.dp)
                             .height(80.dp)
                             .width(120.dp),
                         shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                         elevation = CardDefaults.cardElevation(4.dp)
                     ) {
                         Column(
                             modifier = Modifier
                                 .fillMaxSize()
                                 .padding(8.dp),
                             verticalArrangement = Arrangement.Center,
                             horizontalAlignment = Alignment.CenterHorizontally
                         ) {
                             Text(stock.symbol.uppercase(), fontWeight = FontWeight.Bold)
                             Text("Δ ${(stock.price - prevPrice).format(2)}", fontSize = 14.sp)
                             Text("₹ ${stock.price.format(2)}", fontWeight = FontWeight.SemiBold)
                         }
                     }
                 }
             }*/

            // Portfolio card
            Spacer(Modifier.height(24.dp))
            Text("Your Portfolio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E9)),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEDD3))
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stock (Name)", fontWeight = FontWeight.SemiBold, fontSize = titleSize, modifier = Modifier.weight(2f), color = Color(0xFF2B2B2B))
                        Text("Qty", fontWeight = FontWeight.SemiBold, fontSize = titleSize, modifier = Modifier.weight(1f), color = Color(0xFF2B2B2B))
                        Text("Avg", fontWeight = FontWeight.SemiBold, fontSize = titleSize, modifier = Modifier.weight(1f), color = Color(0xFF2B2B2B))
                        Text("Now", fontWeight = FontWeight.SemiBold, fontSize = titleSize, modifier = Modifier.weight(1f), color = Color(0xFF2B2B2B))
                        Text("Invested", fontWeight = FontWeight.SemiBold, fontSize = titleSize, modifier = Modifier.weight(1f), color = Color(0xFF2B2B2B))
                        Text("Value", fontWeight = FontWeight.SemiBold, fontSize = titleSize, modifier = Modifier.weight(1f), color = Color(0xFF2B2B2B))
                        Text("P/L", fontWeight = FontWeight.SemiBold, fontSize = titleSize, modifier = Modifier.weight(1f), color = Color(0xFF2B2B2B))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(portfolio) { line: StocksRepository.PortfolioLine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${line.name} (${line.symbol})", fontSize = fCell, modifier = Modifier.weight(2f), color = Color(0xFF333333))
                                Text("${line.qty}", fontSize = fCell, modifier = Modifier.weight(1f), color = Color(0xFF333333))
                                Text(line.avgPrice.format(2), fontSize = fCell, modifier = Modifier.weight(1f), color = Color(0xFF333333))
                                Text(line.currentPrice.format(2), fontSize = fCell, modifier = Modifier.weight(1f), color = Color(0xFF333333))
                                Text(line.invested.format(2), fontSize = fCell, modifier = Modifier.weight(1f), color = Color(0xFF333333))
                                Text(line.currentValue.format(2), fontSize = fCell, modifier = Modifier.weight(1f), color = Color(0xFF333333))
                                Text(
                                    line.pnl.format(2),
                                    fontSize = fCell,
                                    color = if (line.pnl >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Divider(color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }



            Spacer(Modifier.height(16.dp))

            if (userViewModel.isAdmin) {
                Button(
                    onClick = { navControllerHome.navigate("AdminStockScreen") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Stock")
                }
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
