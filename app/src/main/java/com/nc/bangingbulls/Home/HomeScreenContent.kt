package com.nc.bangingbulls.Home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nc.bangingbulls.Authentication.AuthViewModel
import com.nc.bangingbulls.Home.Stocks.Leaderboard.LeaderboardScreen
import com.nc.bangingbulls.Home.Stocks.Leaderboard.LeaderboardViewModel
import com.nc.bangingbulls.Home.Stocks.StockFiles.PortfolioItem
import com.nc.bangingbulls.Home.Stocks.StockFiles.StocksViewModel

@Composable
fun HomeScreenContent(
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    navControllerHome: NavController,
    stocksViewModel: StocksViewModel
) {
    val coins = (userViewModel.coins as? Long) ?: (userViewModel.coins as? Int)?.toLong() ?: 0L

    val animatedCoins by animateFloatAsState(targetValue = coins.toFloat(), animationSpec = tween(600))

    val holdings by remember { derivedStateOf { userViewModel.holdings } }
    val stocks by remember { derivedStateOf { stocksViewModel.stocks.value } }
 //   var leaderboard by remember { derivedStateOf { userViewModel.leaderboard } }

    val portfolio = holdings.mapNotNull { holding ->
        val stock = stocks.find { it.id == holding.stockId } ?: return@mapNotNull null
        PortfolioItem(stock.name, stock.symbol, holding.qty, stock.price, holding.avgPrice)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Welcome, ${userViewModel.username}!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("💰 $animatedCoins", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

        }

        Spacer(Modifier.height(24.dp))

        // Leaderboard
        Text("Leaderboard", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        val leaderboardViewModel = viewModel<LeaderboardViewModel>()
        LeaderboardScreen(leaderboardViewModel)
       /* LazyColumn {
            items(leaderboard) { user ->
                Card(modifier = Modifier.padding(4.dp).fillMaxWidth()) {
                    Row(
                        Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(user.username)
                        Text("${user.totalCoins}")
                    }
                }
            }
        }*/

        Spacer(Modifier.height(24.dp))

        // Profile
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            if (userViewModel.profileUrl != null) {
                // Image(painter = rememberAsyncImagePainter(userViewModel.profileUrl),
                //       contentDescription = "Profile",
                //       modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Icon(Icons.Default.Person, contentDescription = "Default Profile", modifier = Modifier.fillMaxSize())
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Popular Stocks", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(stocks) { stock ->
                val prevPrice = stock.priceHistory.lastOrNull()?.price ?: stock.price
                Text(
                    "${stock.symbol} ↑${(stock.price - prevPrice).format(2)}",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        val uid = userViewModel.auth.currentUser?.uid ?: return
        Text("Your Portfolio", fontWeight = FontWeight.Bold)
        HomePortfolioSection(uid)

        if(userViewModel.isAdmin){
            Button(onClick = { navControllerHome.navigate("AdminStockScreen") }) {
                Text("Create Stock")
            }


        }

    }

}


fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Composable
fun PortfolioScreen(viewModel: StocksViewModel, uid: String) {
    val lines by viewModel.portfolio.collectAsState()
    LaunchedEffect(uid) { viewModel.loadPortfolio(uid) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp)
    ) {
        items(lines) { line ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${line.name} (${line.symbol})", fontWeight = FontWeight.Bold)
                    Text("Qty: ${line.qty}  Avg: ${"%.2f".format(line.avgPrice)}  Now: ${"%.2f".format(line.currentPrice)}")
                    Text("Invested: ${"%.2f".format(line.invested)}  Value: ${"%.2f".format(line.currentValue)}")
                    val color = if (line.pnl >= 0) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFC62828)
                    Text("P/L: ${"%.2f".format(line.pnl)}", color = color)
                }
            }
        }
    }
}