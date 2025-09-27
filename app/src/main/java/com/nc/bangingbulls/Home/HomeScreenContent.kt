package com.nc.bangingbulls.Home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nc.bangingbulls.Authentication.AuthViewModel
import com.nc.bangingbulls.Home.Stocks.PortfolioItem
import com.nc.bangingbulls.stocks.StocksViewModel

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
    val leaderboard by remember { derivedStateOf { userViewModel.leaderboard } }

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
        LazyColumn {
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
        }

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
        if(userViewModel.isAdmin){
            Button(onClick = { navControllerHome.navigate("AdminStockScreen") }) {
                Text("Create Stock")
            }
        }
    }

}


fun Double.format(digits: Int) = "%.${digits}f".format(this)
