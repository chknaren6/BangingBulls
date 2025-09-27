package com.nc.bangingbulls.Home.Game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nc.bangingbulls.Home.UserViewModel
@Composable
fun GameScreen(navController: NavController, userViewModel: UserViewModel) {
    val userCoins = userViewModel.coins

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Coins: $userCoins", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))

        val games = listOf(
            "Crash / Rocket Game" to "crashGame",
            "Dice / Hi-Lo" to "diceGame",
            "Limbo" to "limboGame",
            "Coin Flip" to "coinFlipGame"
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            games.forEach { (title, route) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(route) },
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
