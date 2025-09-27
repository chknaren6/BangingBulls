package com.nc.bangingbulls.Home.Game

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.nc.bangingbulls.Home.UserViewModel
import kotlinx.coroutines.delay
import com.nc.bangingbulls.R
import com.nc.bangingbulls.playSoundFromAssets
import kotlin.random.Random

@Composable
fun CrashGameScreen(userViewModel: UserViewModel) {
    val userCoins = userViewModel.coins  // Assuming coins is StateFlow<Long>
    var bet by remember { mutableStateOf("") }
    val context = LocalContext.current
    val tag = "CrashGame"

    val scratchPlayer by remember { mutableStateOf(preloadSound(context, "scratch.mp3")) }
    val coinPlayer by remember { mutableStateOf(preloadSound(context, "coin.mp3")) }

    val cardCount = 10
    val revealedStates = remember { mutableStateListOf<Boolean>().apply { repeat(cardCount) { add(false) } } }

    val pagerState = rememberPagerState(pageCount = { cardCount })

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        HorizontalPager(state = pagerState) { index ->
            var betAmount by remember { mutableStateOf(0L) }
            var multiplierValue by remember { mutableStateOf(1f) }
            var isFinished by remember { mutableStateOf(false) }

            val revealed = revealedStates[index]

            // Lottie composition
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset("scratch.json"))
            val progress by animateLottieCompositionAsState(
                composition,
                isPlaying = revealed && !isFinished,
                iterations = 1
            )

            // Fallback for animation stall
            LaunchedEffect(revealed) {
                if (revealed && !isFinished) {
                    delay(3000)  // 3s timeout
                    if (!isFinished) isFinished = true
                }
            }

            LaunchedEffect(progress) {
                if (progress >= 0.99f) {
                    isFinished = true
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = !revealed) {
                        playSoundFromAssets(scratchPlayer, "scratch.mp3")
                        val enteredBet = bet.toLongOrNull() ?: 0L
                        if (enteredBet > 0 && enteredBet <= userCoins) {
                            betAmount = enteredBet
                            // Multiplier: 0x to 10x, slightly win-biased
                            multiplierValue = Random.nextFloat() * 8f
                            val winAmount = (betAmount * multiplierValue).toLong()
                            if(winAmount<betAmount){
                                val netChange= -betAmount
                                userViewModel.updateCoins(netChange)
                                playSoundFromAssets(context, "lose.mp3")
                            }
                            val netChange = winAmount - betAmount.toLong()
                            Log.d(tag, "Card $index: Bet=$enteredBet, Multiplier=${String.format("%.1f", multiplierValue)}x, Win=$winAmount, Net=$netChange")
                            revealedStates[index] = true
                            // Apply net change (win - bet) atomically
                            userViewModel.updateCoins(netChange)
                            playSoundFromAssets(context, "scratch.mp3")
                        }
                    },
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !revealed -> Color.Gray
                        multiplierValue > 1f -> Color.Green
                        multiplierValue < 1f -> Color.Red
                        else -> Color.Yellow
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!revealed) {
                        Text("Scratch Me!", fontSize = 24.sp, color = Color.White)
                    } else {
                        if (!isFinished) {
                            LottieAnimation(
                                composition,
                                progress = progress,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val winChance = 0.6
                            val isWin = Random.nextFloat() < winChance
                            multiplierValue = if (isWin) Random.nextFloat() * 2f + 1f else Random.nextFloat() * 0.5f // 1–3× if win, 0–0.5× if lose
                            val winAmount = (betAmount * multiplierValue).toInt()
                            val netChange = winAmount - betAmount.toInt()
                            userViewModel.updateCoins(netChange.toLong())

                            Text(
                                "${String.format("%.1f", multiplierValue)}x\nWin: $winAmount\nNet: ${if (netChange > 0) "+$netChange" else netChange}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            LaunchedEffect(Unit) {
                                delay(3000)  // Show result
                                Log.d(tag, "Processed card $index, net change: $netChange")
                                playSoundFromAssets(context, "coin.mp3")
                                bet = ""  // Reset bet after result
                            }
                        }
                    }
                }
            }
        }

        // Top overlay for coins/bet (like iOS notch)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(16.dp)
        ) {
            Text("Coins: $userCoins", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = bet,
                onValueChange = { bet = it.filter { ch -> ch.isDigit() } },
                label = { Text("Bet per Card", color = Color.White) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scratchPlayer?.release()
            coinPlayer?.release()
        }
    }
}



// Preload MediaPlayer to reduce latency
fun preloadSound(context: Context, fileName: String): MediaPlayer? {
    return try {
        val assetFileDescriptor = context.assets.openFd(fileName)
        MediaPlayer().apply {
            setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
            prepare() // Synchronous prep for faster playback
            setOnErrorListener { _, what, extra ->
                Log.e("Audio", "Error in $fileName: what=$what, extra=$extra")
                false
            }
        }.also {
            assetFileDescriptor.close()
        }
    } catch (e: Exception) {
        Log.e("Audio", "Failed to preload $fileName: ${e.message}")
        null
    }
}

// Modified playSoundFromAssets to use preloaded MediaPlayer
fun playSoundFromAssets(player: MediaPlayer?, fileName: String) {
    if (player == null) {
        Log.e("Audio", "MediaPlayer null for $fileName")
        return
    }
    try {
        if (player.isPlaying) {
            player.stop()
            player.prepare()
        }
        player.seekTo(0)
        player.start()
        Log.d("Audio", "Playing $fileName")
    } catch (e: Exception) {
        Log.e("Audio", "Failed to play $fileName: ${e.message}")
    }
}