package com.nc.bangingbulls.Home.Game.V

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nc.bangingbulls.Home.UserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.nc.bangingbulls.R
import com.nc.bangingbulls.playSoundFromAssets

@Composable
fun CoinFlipGameScreen(navController: NavController, userViewModel: UserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Vibrator::class.java)

    var bet by remember { mutableStateOf("") }
    var choice by remember { mutableStateOf("heads") }
    var flipping by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf("heads") }
    var resultText by remember { mutableStateOf("") }
    var floatingText by remember { mutableStateOf<Pair<String, Color>?>(null) }
    var floatingOffset by remember { mutableStateOf(0f) }
    var popScale by remember { mutableStateOf(1f) }

    val userCoins = userViewModel.coins
    val animatedCoins by animateFloatAsState(targetValue = userCoins.toFloat(), animationSpec = tween(600))

    val rotationDegree by animateFloatAsState(
        targetValue = if (flipping) 1080f else 0f,
        animationSpec = tween(durationMillis = 900)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.coinflip),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)))

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top bar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { navController.popBackStack() }) {
                    Text("\u2190", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Back", fontSize = 18.sp, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text("Coins: 💰${animatedCoins.toLong()}", fontWeight = FontWeight.Bold, color = Color.Yellow, fontSize = 18.sp)
            }

            // Bet input
            OutlinedTextField(
                value = bet,
                onValueChange = { bet = it.filter { ch -> ch.isDigit() } },
                label = { Text("Bet amount", color = Color.White) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Choice buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { choice = "heads" },
                    colors = ButtonDefaults.buttonColors(containerColor = if (choice == "heads") Color(0xFF3B68F7) else Color(0xFF223058)),
                    modifier = Modifier.weight(1f)
                ) { Text("Heads") }

                Button(
                    onClick = { choice = "tails" },
                    colors = ButtonDefaults.buttonColors(containerColor = if (choice == "tails") Color(0xFF3B68F7) else Color(0xFF223058)),
                    modifier = Modifier.weight(1f)
                ) { Text("Tails") }
            }

            // Coin animation with pop effect
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .rotate(rotationDegree)
                    .scale(popScale)
                    .background(Color(0xFFFFD700), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(outcome.uppercase(), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 20.sp)

                // Floating win/loss text
                floatingText?.let { (text, color) ->
                    Text(text, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.offset(y = -floatingOffset.dp))
                }
            }

            // Flip button
            Button(
                enabled = bet.toLongOrNull()?.let { it in 1..userCoins } == true && !flipping,
                onClick = {
                    flipping = true
                    resultText = ""
                    val betAmount = bet.toLongOrNull() ?: 0L
                    playSoundFromAssets(context, "flip.mp3") // Flip sound

                    scope.launch {
                        delay(900)
                        outcome = if (Random.nextBoolean()) "heads" else "tails"
                        val won = choice == outcome
                        val coinsChange = if (won) (betAmount * 1.1).toLong() else -(betAmount * 0.9).toLong()

                        if (coinsChange < 0) userViewModel.deductCoins(-coinsChange)
                        else userViewModel.addCoins(coinsChange)

                        // Pop animation on win
                        if (won) {
                            popScale = 1.5f
                            playSoundFromAssets(context, "coin.mp3")
                            vibrator?.vibrate(VibrationEffect.createOneShot(200, 100))
                            delay(150)
                            popScale = 1f
                        } else {
                            playSoundFromAssets(context, "lose.mp3")
                            vibrator?.vibrate(VibrationEffect.createOneShot(150, 50))
                        }

                        resultText = if (won) "You won ${coinsChange} coins!" else "You lost ${-coinsChange} coins!"
                        floatingText = if (won) "+${coinsChange}" to Color(0xFF00FF00) else "${coinsChange}" to Color(0xFFFF4D4D)
                        floatingOffset = 0f
                        launch {
                            repeat(30) {
                                floatingOffset += 4f
                                delay(16)
                            }
                            floatingText = null
                        }

                        flipping = false
                        bet = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (flipping) "Flipping..." else "Flip Coin") }

            // Result text
            if (resultText.isNotEmpty()) {
                Text(resultText, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = if (resultText.contains("won")) Color(0xFF00FF00) else Color(0xFFFF4D4D), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }

            // Random win/lose lines
            val winLine = userViewModel.winLines.randomOrNull() ?: ""
            val loseLine = userViewModel.loseLines.randomOrNull() ?: ""
            if (!flipping && resultText.isNotEmpty()) {
                Text(text = if (resultText.contains("won")) winLine else loseLine, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color(0xFFB3B3B3), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
