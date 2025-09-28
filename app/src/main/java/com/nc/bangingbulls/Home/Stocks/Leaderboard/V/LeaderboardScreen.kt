package com.nc.bangingbulls.Home.Stocks.Leaderboard.V


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nc.bangingbulls.Home.Stocks.Leaderboard.VM.LeaderboardViewModel

@Composable
fun LeaderboardScreen(leaderboardViewModel: LeaderboardViewModel) {
    val rows by leaderboardViewModel.rows.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        itemsIndexed(rows) { index, row ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("#${index + 1}  ${row.username}", fontWeight = FontWeight.Bold)
                        Text("Portfolio: ${"%.2f".format(row.portfolio)}  Coins: ${"%.2f".format(row.coins)}")
                    }
                    Text("Total: ${"%.2f".format(row.totalCoins)}")
                }
            }
        }
    }
}