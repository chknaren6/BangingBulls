package com.nc.bangingbulls.Home.Game.V

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.R
import com.nc.bangingbulls.playSoundFromAssets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LimboGameScreen(navController: NavController, userViewModel: UserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Vibrator::class.java)

    var bet by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(1f) }
    var crashPoint by remember { mutableStateOf(0f) }
    var rolling by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var floatingText by remember { mutableStateOf<Pair<String, Color>?>(null) }
    var floatingOffset by remember { mutableStateOf(0f) }
    var winAnimation by remember { mutableStateOf(false) }
    var loseAnimation by remember { mutableStateOf(false) }
    var playScale by remember { mutableStateOf(1f) }

    val userCoins = userViewModel.coins
    val animatedCoins by animateFloatAsState(
        targetValue = userCoins.toFloat(), animationSpec = tween(600), label = ""
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.limbobg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()/*.background(
                    if (winAnimation) Color(0xFF1B5E20)
                    else if (loseAnimation) Color(0xFFB71C1C)
                    else Color(0xFF121931)
                )*/.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { navController.popBackStack() }) {
                    Text(
                        text = "\u2190",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Back", fontSize = 18.sp, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Coins: 💰${animatedCoins.toLong()}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow,
                    fontSize = 18.sp
                )
            }
            OutlinedTextField(
                value = bet,
                onValueChange = { bet = it.filter { ch -> ch.isDigit() } },
                label = { Text("Bet amount", color = Color(0xFF8FA3BF)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF121931),
                    focusedContainerColor = Color(0xFF121931),
                    unfocusedBorderColor = Color(0xFF223058),
                    focusedBorderColor = Color(0xFF3B68F7),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedLabelColor = Color(0xFF8FA3BF),
                    focusedLabelColor = Color(0xFF8FA3BF)
                )
            )

            Text("Pick Multiplier: ${String.format("%.1f", multiplier)}x", color = Color.White)
            Slider(
                value = multiplier,
                onValueChange = { multiplier = it },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = !rolling && bet.toLongOrNull()
                ?.let { it > 0 && it <= userCoins } == true,
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

                        val win = multiplier <= crashPoint
                        val net = if (win) (betAmount * multiplier).toLong() else -betAmount

                        if (win) {
                            userViewModel.addCoins(net)
                            playSoundFromAssets(context, "coin.mp3")
                            vibrator?.vibrate(VibrationEffect.createOneShot(200, 100))
                            floatingText = "+$net" to Color(0xFFB6F5C5)
                            winAnimation = true
                            resultText = userViewModel.winLines.random()
                        } else {
                            playSoundFromAssets(context, "lose.mp3")
                            vibrator?.vibrate(VibrationEffect.createOneShot(150, 50))
                            floatingText = "$net" to Color(0xFFFFB3B3)
                            loseAnimation = true
                            resultText = userViewModel.loseLines.random()
                        }

                        floatingOffset = 0f
                        scope.launch {
                            repeat(30) {
                                floatingOffset += 4f
                                delay(16)
                            }
                            floatingText = null
                        }

                        rolling = false
                        bet = ""
                    }
                },
                modifier = Modifier
                    .scale(playScale)
                    .fillMaxWidth()
            ) {
                Text("Play")
            }

            floatingText?.let { (text, color) ->
                Text(
                    text = text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.offset(y = -floatingOffset.dp)
                )
            }

            if (resultText.isNotEmpty()) {
                Text(
                    resultText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = if (winAnimation) Color(0xFF00FF00) else if (loseAnimation) Color(
                        0xFFFF4D4D
                    ) else Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
