package com.nc.bangingbulls.Home.Game.V

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.R
import com.nc.bangingbulls.playSoundFromAssets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun DiceGameScreen(userViewModel: UserViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Vibrator::class.java)
    val random = Random.Default
    var diceNumber by remember { mutableStateOf(1) }
    var bet by remember { mutableStateOf("") }
    var choice by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var rolling by remember { mutableStateOf(false) }
    var floatingText by remember { mutableStateOf<Pair<String, Color>?>(null) }
    var floatingOffset by remember { mutableStateOf(0f) }
    var resultText by remember { mutableStateOf("") }

    val totalCoins = userViewModel.coins
    val betValue = bet.toLongOrNull() ?: 0L
    val canGamble = betValue in 1..totalCoins && choice.isNotEmpty()
    val multiplier = 2
    val scopeFloating = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121931))
    ) {
        Image(
            painter = painterResource(id = R.drawable.dicebg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u2190 Back",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Coins: 💰$totalCoins", color = Color.Yellow, fontWeight = FontWeight.Bold
                )
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1320)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Place your bet", color = Color(0xFFA9B1C3), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bet,
                        onValueChange = { bet = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        label = { Text("Bet amount", color = Color(0xFF8FA3BF)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF121931),
                            focusedContainerColor = Color(0xFF121931),
                            unfocusedBorderColor = Color(0xFF223058),
                            focusedBorderColor = Color(0xFF3B68F7),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { choice = "High" }, colors = ButtonDefaults.buttonColors(
                                containerColor = if (choice == "High") Color(0xFF3B68F7) else Color(
                                    0xFF223058
                                )
                            ), modifier = Modifier.weight(1f)
                        ) { Text("High (4–6)") }

                        Button(
                            onClick = { choice = "Low" }, colors = ButtonDefaults.buttonColors(
                                containerColor = if (choice == "Low") Color(0xFF3B68F7) else Color(
                                    0xFF223058
                                )
                            ), modifier = Modifier.weight(1f)
                        ) { Text("Low (1–3)") }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        enabled = canGamble && !rolling,
                        onClick = { showConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Gamble") }
                }
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(180.dp)) {
                Image(
                    painter = painterResource(
                        id = when (diceNumber) {
                            1 -> R.drawable.dice_1
                            2 -> R.drawable.dice_2
                            3 -> R.drawable.dice_3
                            4 -> R.drawable.dice_4
                            5 -> R.drawable.dice_5
                            else -> R.drawable.dice_6
                        }
                    ), contentDescription = "Dice Face", modifier = Modifier.size(150.dp)
                )

                floatingText?.let { (text, color) ->
                    Text(
                        text = text,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.offset(y = -floatingOffset.dp)
                    )
                }
            }

            if (resultText.isNotEmpty()) {
                Text(
                    text = resultText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        if (showConfirm) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {}) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    tonalElevation = 6.dp
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(22.dp)
                                .clickable { showConfirm = false },
                            tint = Color(0xFF666C7A)
                        )

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Confirm bet",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = Color(0xFF0E1320)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Play with bet: $bet on $choice?", color = Color(0xFF516079))
                            Spacer(Modifier.height(16.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canGamble,
                                onClick = {
                                    showConfirm = false
                                    rolling = true
                                    userViewModel.deductCoins(betValue)
                                    vibrator?.vibrate(VibrationEffect.createOneShot(100, 50))
                                    playSoundFromAssets(context, "roll.mp3")

                                    scope.launch {
                                        rolling = true
                                        userViewModel.deductCoins(betValue)
                                        vibrator?.vibrate(VibrationEffect.createOneShot(100, 50))
                                        playSoundFromAssets(context, "roll.mp3")

                                        repeat(8) {
                                            diceNumber = random.nextInt(1, 7)
                                            delay(150)
                                        }

                                        val win =
                                            (choice == "High" && diceNumber in 4..6) || (choice == "Low" && diceNumber in 1..3)
                                        val net = if (win) betValue * multiplier else -betValue

                                        if (win) {
                                            userViewModel.addCoins(net)
                                            playSoundFromAssets(context, "coin.mp3")
                                            vibrator?.vibrate(
                                                VibrationEffect.createOneShot(
                                                    200,
                                                    100
                                                )
                                            )
                                        } else {
                                            playSoundFromAssets(context, "lose.mp3")
                                            vibrator?.vibrate(
                                                VibrationEffect.createOneShot(
                                                    150,
                                                    50
                                                )
                                            )
                                        }

                                        val resultColor =
                                            if (win) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        floatingText =
                                            Pair(if (win) "+$net" else "-$betValue", resultColor)

                                        scopeFloating.launch {
                                            floatingOffset = 0f
                                            delay(200)
                                            while (floatingOffset < 100) {
                                                floatingOffset += 2
                                                delay(10)
                                            }
                                            floatingText = null
                                        }

                                        resultText =
                                            if (win) "You won $net coins!" else "You lost $betValue coins."
                                        rolling = false
                                    }
                                },
                            ) { Text("Confirm") }
                        }
                    }
                }
            }
        }
    }
}
