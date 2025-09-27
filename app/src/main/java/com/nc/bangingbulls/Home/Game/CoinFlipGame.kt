package com.nc.bangingbulls.Home.Game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nc.bangingbulls.Home.UserViewModel
import kotlin.random.Random

@Composable
fun CoinFlipGameScreen(navController: NavController, userViewModel: UserViewModel) {
    var bet by remember { mutableStateOf("") }
    var choice by remember { mutableStateOf("heads") }
    var resultText by remember { mutableStateOf("") }
    var flipping by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf("heads") }
    var flipDegree by remember { mutableStateOf(0f) }
    val userCoins = userViewModel.coins
    var pendingFlip by remember { mutableStateOf(false) }

    val animatedRotation by animateFloatAsState(
        targetValue = if (flipping) 1080f else 0f, // 3 full spins
        animationSpec = tween(durationMillis = 900), label = "spin"
    )

    // Win/Loss ratios for addictive gameplay and minimal risk
    val WIN_RATIO = 1.1
    val LOSS_RATIO = 0.9

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your Coins: $userCoins", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = bet,
            onValueChange = { bet = it.filter { ch -> ch.isDigit() } },
            label = { Text("Bet (Coins)") }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { choice = "heads" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (choice == "heads") Color(0xFFB3E5FC) else MaterialTheme.colorScheme.primary
                )
            ) { Text("Heads") }

            Button(
                onClick = { choice = "tails" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (choice == "tails") Color(0xFFB3E5FC) else MaterialTheme.colorScheme.primary
                )
            ) { Text("Tails") }
        }

        // Coin Flip Animation
        Box(modifier = Modifier
            .size(100.dp)
            .rotate(animatedRotation)
            .background(Color(0xFFFFD700), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = outcome.uppercase(),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Button(
            enabled = bet.toLongOrNull()?.let { it in 1..userCoins } == true && !flipping,
            onClick = {
                pendingFlip = true
                resultText = ""
            }
        ) {
            Text(if (flipping) "Flipping..." else "Flip Coin")
        }

        LaunchedEffect(pendingFlip) {
            if (pendingFlip) {
                flipping = true
                kotlinx.coroutines.delay(900)
                val betAmount = bet.toLongOrNull() ?: 0L
                outcome = if (Random.nextBoolean()) "heads" else "tails"
                val won = choice == outcome

                val coinsChange = if (won) (betAmount * WIN_RATIO).toLong() else -(betAmount * LOSS_RATIO).toLong()
                if (coinsChange < 0) userViewModel.deductCoins(-coinsChange)
                else userViewModel.addCoins(coinsChange)

                resultText = if (won) {
                    "Outcome: $outcome. You won ${(betAmount * WIN_RATIO).toLong()} coins!"
                } else {
                    "Outcome: $outcome. You lost ${(betAmount * LOSS_RATIO).toLong()} coins!"
                }
                flipping = false
                pendingFlip = false
            }
        }


        AnimatedVisibility(visible = resultText.isNotBlank()) {
            Text(resultText, fontWeight = FontWeight.Bold, color = if (resultText.contains("won")) Color(0xFF388E3C) else Color(0xFFD32F2F))
        }
    }
}
