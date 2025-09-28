package com.nc.bangingbulls.Home.Game.V

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.playSoundFromAssets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@Composable
fun LimboGameScreen(navController: NavController, userViewModel: UserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bet by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(1f) }
    var crashPoint by remember { mutableStateOf(0f) }
    var rolling by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var winAnimation by remember { mutableStateOf(false) }
    var loseAnimation by remember { mutableStateOf(false) }

    var showCoins by remember { mutableStateOf(true) }

    val userCoins = userViewModel.coins
    val animatedCoins by animateFloatAsState(targetValue = userCoins.toFloat(), animationSpec = tween(600))

    val vibrator = context.getSystemService(Vibrator::class.java)

    // Multiplier rising Lottie
   // val composition by rememberLottieComposition(LottieCompositionSpec.Asset("limbo_multiplier.json"))
   /* val progress by animateLottieCompositionAsState(
        composition,
        isPlaying = rolling,
        iterations = 1
    )*/

    // Button click scale animations
    var playScale by remember { mutableStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (winAnimation) Color(0xFF2E7D32) else if (loseAnimation) Color(0xFFC62828) else Color(0xFF222222))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // Coins display
        Text(
            "Coins: ${animatedCoins.toLong()}",
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Color.Yellow
        )

        // Bet input
        OutlinedTextField(
            value = bet,
            onValueChange = { bet = it.filter { ch -> ch.isDigit() } },
            label = { Text("Bet") },
            singleLine = true
        )

        // Multiplier slider
        Text("Pick Multiplier: ${String.format("%.1f", multiplier)}x", color = Color.White)
        Slider(
            value = multiplier,
            onValueChange = { multiplier = it },
            valueRange = 1f..10f,
            steps = 8
        )

        // Play button
        Button(
            enabled = !rolling && bet.toLongOrNull()?.let { it <= userCoins } == true,
            onClick = {
                playScale = 1.2f
                scope.launch { delay(100); playScale = 1f }

                rolling = true
                resultText = ""
                winAnimation = false
                loseAnimation = false

                val betAmount = bet.toLongOrNull() ?: 0L
                userViewModel.deductCoins(betAmount)
                crashPoint = (1..10).random().toFloat()

                scope.launch {
                    var currentMultiplier = 1f
                    while (currentMultiplier < crashPoint && currentMultiplier < multiplier) {
                        currentMultiplier += 0.1f
                        playSoundFromAssets(context, "roll.mp3")
                        delay(50)
                    }

                    if (multiplier <= crashPoint) {
                        val winAmount = (betAmount * multiplier).toLong()
                        userViewModel.addCoins(winAmount)
                        playSoundFromAssets(context, "coin.mp3")
                        vibrator?.vibrate(VibrationEffect.createOneShot(200, 100))
                        winAnimation = true
                        resultText = "You won $winAmount coins!"

                        // Show coins in UI
                        showCoins = true
                    } else {
                        playSoundFromAssets(context, "lose.mp3")
                        vibrator?.vibrate(VibrationEffect.createOneShot(150, 50))
                        loseAnimation = true
                        resultText = "You lost!"
                    }
                    rolling = false
                }
            },
            modifier = Modifier.scale(playScale)
        ) {
            Text("Play")
        }
       /* if (showCoins) {
            FlyingCoinsAnimation(
                startX = 100f,
                startY = 400f,
                endX = 0f,
                endY = 0f,
                count = 10
            ) {
                showCoins = false // Hide when finished
            }
        }*/

        // Lottie multiplier animation during rolling
        if (rolling) {
          /*  LottieAnimation(
                composition = composition,
                progress = progress,
                modifier = Modifier.size(150.dp)
            )*/
            Text("Multiplier Rising...", fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Result display
        if (resultText.isNotEmpty()) {
            Text(
                resultText,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = if (winAnimation) Color.Green else if (loseAnimation) Color.Red else Color.White
            )
        }
    }
}

/*
@Composable
fun FlyingCoinsAnimation(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    count: Int = 5,
    onComplete: () -> Unit
) {
    val coins = remember { mutableStateListOf<Float>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repeat(count) {
            coins.add(it.toFloat())
        }
    }

    coins.forEachIndexed { index, _ ->
        var x by remember { mutableStateOf(startX) }
        var y by remember { mutableStateOf(startY) }

        LaunchedEffect(index) {
            val steps = 20
            repeat(steps) { step ->
                x = startX + (endX - startX) * (step / steps.toFloat())
                y = startY + (endY - startY) * (step / steps.toFloat())
                kotlinx.coroutines.delay(20)
            }
            if (index == coins.lastIndex) onComplete()
        }

        Image(
            painter = painterResource(id = R.drawable.gamecoin),
            contentDescription = "Coin",
            modifier = Modifier
                .size(24.dp)
                .offset(x.dp, y.dp)
        )
    }
}*/
