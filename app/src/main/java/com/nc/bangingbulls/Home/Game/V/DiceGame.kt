package com.nc.bangingbulls.Home.Game.V

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.R
import com.nc.bangingbulls.playSoundFromAssets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun DiceGameScreen(navController: NavController, userViewModel: UserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Vibrator::class.java)
    val random = Random.Default

    var diceNumber by remember { mutableStateOf(1) }
    var resultText by remember { mutableStateOf("") }
    var rolling by remember { mutableStateOf(false) }
    val betAmount = 10L

    // Animated coins
    val coins = userViewModel.coins
    val animatedCoins by animateFloatAsState(
        targetValue = coins.toFloat(),
        animationSpec = tween(600)
    )

    // Button scale animation
    var higherScale by remember { mutableStateOf(1f) }
    var lowerScale by remember { mutableStateOf(1f) }

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("dice_roll.json"))
    val progress by animateLottieCompositionAsState(
        composition,
        isPlaying = rolling,
        iterations = 1
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Coins Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Coins: ${animatedCoins.toLong()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow
            )
        }

        // Dice Display (with image)
        Image(
            painter = painterResource(
                id = when(diceNumber) {
                    1 -> R.drawable.dice_1
                    2 -> R.drawable.dice_2
                    3 -> R.drawable.dice_3
                    4 -> R.drawable.dice_4
                    5 -> R.drawable.dice_5
                    else -> R.drawable.dice_6
                }
            ),
            contentDescription = "Dice Face",
            modifier = Modifier.size(150.dp)
        )


        // Result
        if (resultText.isNotEmpty()) {
            Text(
                text = resultText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (resultText.contains("Win")) Color.Green else Color.Red
            )
        }

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                enabled = !rolling && coins >= betAmount,
                onClick = {
                    higherScale = 1.1f
                    scope.launch {
                        delay(100)
                        higherScale = 1f
                    }

                    rolling = true
                    resultText = ""
                    userViewModel.deductCoins(betAmount.toLong())
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, 50))
                    playSoundFromAssets(context, "roll.mp3")

                    scope.launch {
                        repeat(8) {
                            diceNumber = random.nextInt(1, 7)
                            delay(80)
                        }
                        val win = diceNumber > 3
                        if (win) {
                            userViewModel.addCoins(20)
                            playSoundFromAssets(context, "coin.mp3")
                            vibrator?.vibrate(VibrationEffect.createOneShot(200, 100))
                        } else {
                            playSoundFromAssets(context, "lose.mp3")
                            vibrator?.vibrate(VibrationEffect.createOneShot(150, 50))
                        }
                        resultText = if (win) "You Win!" else "You Lose!"
                        rolling = false
                    }
                },
                modifier = Modifier.scale(higherScale)
            ) {
                Text("Roll Higher")
            }

            Button(
                enabled = !rolling && coins >= betAmount,
                onClick = {
                    lowerScale = 1.1f
                    scope.launch {
                        delay(100)
                        lowerScale = 1f
                    }

                    rolling = true
                    resultText = ""
                    userViewModel.deductCoins(betAmount.toLong())
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, 50))
                    playSoundFromAssets(context, "roll.mp3")

                    scope.launch {
                        repeat(8) {
                            diceNumber = random.nextInt(1, 7)
                            delay(80)
                        }
                        val win = diceNumber < 3
                        if (win) {
                            userViewModel.addCoins(20)
                            playSoundFromAssets(context, "coin.mp3")
                            vibrator?.vibrate(VibrationEffect.createOneShot(200, 100))
                        } else {
                            playSoundFromAssets(context, "lose.mp3")
                            vibrator?.vibrate(VibrationEffect.createOneShot(150, 50))
                        }
                        resultText = if (win) "You Win!" else "You Lose!"
                        rolling = false
                    }
                },
                modifier = Modifier.scale(lowerScale)
            ) {
                Text("Roll Lower")
            }
        }
    }
}
