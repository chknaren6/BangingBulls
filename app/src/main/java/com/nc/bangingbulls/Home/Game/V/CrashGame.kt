package com.nc.bangingbulls.Home.Game.V

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@Composable
fun CrashGameScreen(userViewModel: UserViewModel, onBack: () -> Unit) {

    LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenW = configuration.screenWidthDp.dp
    val horizontalPad = (screenW * 0.04f).coerceAtLeast(12.dp)
    val corner = 16.dp

    val totalCoins = userViewModel.coins
    var playsToday by remember { mutableStateOf(0) }
    var lastResetAt by remember { mutableStateOf(0L) }
    var bet by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var currentMultiplier by remember { mutableStateOf(1.0) }
    var lastNet by remember { mutableStateOf(0L) }
    var scratching by remember { mutableStateOf(false) }
    var isLoss by remember { mutableStateOf(false) }
    var confirmPressed by remember { mutableStateOf(false) }
    var coinBurst by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userViewModel.observeCrashState { p, ts ->
            playsToday = p
            lastResetAt = ts
        }
    }

    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val millisLeft = (lastResetAt + 24L * 3600_000L) - now
    val locked = playsToday >= 10 && millisLeft > 0

    val ctx = LocalContext.current
    val scratchPlayer by remember(ctx) { mutableStateOf(preloadSound(ctx, "scratch.mp3")) }
    val coinPlayer by remember(ctx) { mutableStateOf(preloadSound(ctx, "coin.mp3")) }
    val lossPlayer by remember(ctx) { mutableStateOf(preloadSound(ctx, "lose.mp3")) }

    var resultQuote by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val coins = userViewModel.coins
    val betValue = bet.toLongOrNull() ?: 0L
    val canGamble = !locked && betValue > 0L && betValue <= coins

    val confirmScale by animateFloatAsState(
        targetValue = if (confirmPressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "confirmScale"
    )
    val coinOpacity by animateFloatAsState(
        targetValue = if (coinBurst) 1f else 0f, animationSpec = tween(300), label = "coinOpacity"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.crashbg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 92.dp, start = horizontalPad, end = horizontalPad, bottom = 24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(corner),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1320)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Place your bet", color = Color(0xFFA9B1C3), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = bet,
                            onValueChange = { bet = it.filter { ch -> ch.isDigit() } },
                            singleLine = true,
                            label = { Text("Bet amount", color = Color(0xFF8FA3BF)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
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
                        Spacer(Modifier.width(10.dp))
                        Button(
                            enabled = canGamble,
                            onClick = { showConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canGamble) Color(0xFF3B68F7) else Color(
                                    0xFF2A3558
                                )
                            )
                        ) { Text("Gamble") }
                    }
                    if (betValue > coins) {
                        Spacer(Modifier.height(6.dp))
                        Text("Insufficient balance", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((screenW * 0.48f).coerceAtLeast(200.dp)),
                shape = RoundedCornerShape(corner),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        scratching -> Color(0xFF1C243E)
                        showResult && lastNet >= 0 -> Color(0xFF0E5E2F)
                        showResult && lastNet < 0 -> Color(0xFF7E1C1C)
                        isLoss -> Color(0xFF7E1C1C)
                        else -> Color(0xFF121931)
                    }
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        locked -> {
                            val secs = (millisLeft / 1000).coerceAtLeast(0)
                            val hh = secs / 3600
                            val mm = (secs % 3600) / 60
                            val ss = secs % 60
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Daily limit reached",
                                    color = Color(0xFFE0E6F3),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    color = Color(0xFF0E1320), shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(
                                        "Resets in %02d:%02d:%02d".format(hh, mm, ss),
                                        color = Color(0xFF9EB1D6),
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        scratching -> {
                            Text("Scratching...", color = Color(0xFFE0E6F3), fontSize = 16.sp)
                        }

                        showResult -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    "${"%.2f".format(currentMultiplier)}x",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Net: ${if (lastNet >= 0) "+$lastNet" else "$lastNet"}",
                                    color = if (lastNet >= 0) Color(0xFFB6F5C5) else Color(
                                        0xFFFFB3B3
                                    ),
                                    fontSize = 16.sp
                                )
                                resultQuote?.let { q ->
                                    Spacer(Modifier.height(10.dp))
                                    Divider(
                                        color = Color.White.copy(alpha = 0.15f),
                                        thickness = 1.dp,
                                        modifier = Modifier.fillMaxWidth(0.66f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }

                        else -> {
                            Text(
                                "Tap Gamble to play\n(plays left: ${10 - playsToday})",
                                color = Color(0xFFB7C3DB),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (coinOpacity > 0f) {
                        Text(
                            "＋$lastNet",
                            color = Color(0xFFB6F5C5),
                            fontSize = 22.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .alpha(coinOpacity)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            resultQuote?.let { q ->
                QuotePanel(
                    text = q, positive = lastNet >= 0, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp)
                .align(Alignment.TopCenter),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPad, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color(0x11000000), shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0E1320)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Back", color = Color(0xFF0E1320))
                    }
                }

                Surface(
                    color = Color(0xFF0E1320), shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "Coins: 💰$totalCoins",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = Color(0x11000000), shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "${minOf(playsToday, 10)}/10",
                        color = Color(0xFF0E1320),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showConfirm) {
        val newCoinsSnap = userViewModel.coins
        val newBet = bet.toLongOrNull() ?: 0L
        val newCanGamble = !locked && newBet > 0L && newBet <= newCoinsSnap


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
                        Text("Play with bet: $newBet ?", color = Color(0xFF516079))
                        Spacer(Modifier.height(16.dp))
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { scaleX = confirmScale; scaleY = confirmScale },
                            enabled = newCanGamble,
                            onClick = {
                                if (!newCanGamble) return@Button
                                confirmPressed = true
                                showConfirm = false
                                scope.launch {
                                    playOneCrash(
                                        bet = newBet,
                                        userViewModel = userViewModel,
                                        onScratch = { scratching = it },
                                        onImmediateLoss = { isLoss = true },
                                        onResult = { mult, net ->
                                            currentMultiplier = mult
                                            lastNet = net
                                            showResult = true
                                            resultQuote =
                                                if (net >= 0) userViewModel.winLines.random() else userViewModel.loseLines.random()


                                            if (net > 0) coinBurst = true

                                            scope.launch {
                                                delay(1200)
                                                showResult = false
                                                isLoss = false
                                                scratching = false
                                                coinBurst = false
                                                confirmPressed = false
                                                bet = ""
                                            }
                                        })
                                }
                            }) { Text("Confirm") }
                    }
                }
            }
        }
    }


    LaunchedEffect(showResult, lastNet) {
        if (showResult) {
            try {
                kotlinx.coroutines.android.awaitFrame()
            } catch (_: Throwable) {
                kotlinx.coroutines.yield()
            }
            if (lastNet > 0) {
                playSoundFromAssets(coinPlayer, "coin.mp3")
            } else if (lastNet < 0) {
                playSoundFromAssets(lossPlayer, "lose.mp3")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scratchPlayer?.release()
            coinPlayer?.release()
            lossPlayer?.release()
        }
    }
}

private fun rollMultiplier(): Double {
    val r = Random.nextDouble()
    return if (r < 0.65) 1.05 + Random.nextDouble(1.45) else Random.nextDouble(0.8)
}


private suspend fun applyCrashOutcomeAtomic(bet: Long, multiplier: Double): Long? {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return null
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val ref = db.collection("users").document(uid)

    return try {
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val coins =
                (snap.getDouble("coins") ?: snap.getLong("coins")?.toDouble() ?: 0.0).toLong()
            if (bet <= 0L || coins < bet) throw IllegalStateException("insufficient")
            val win = kotlin.math.floor(bet * multiplier).toLong()
            val newCoins = coins - bet + win
            if (newCoins < 0L) throw IllegalStateException("negative_final")
            tx.update(ref, "coins", newCoins)
            (win - bet)
        }.await()
    } catch (_: Exception) {
        null
    }
}


private suspend fun playOneCrash(
    bet: Long,
    userViewModel: UserViewModel,
    onScratch: (Boolean) -> Unit,
    onImmediateLoss: () -> Unit,
    onResult: (multiplier: Double, net: Long) -> Unit
) {
    if (bet <= 0L) {
        onResult(1.0, 0L); return
    }

    val allowed = try {
        userViewModel.tryConsumeCrashPlay()
    } catch (_: Exception) {
        false
    }
    if (!allowed) {
        onResult(1.0, 0L); return
    }

    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val userDoc = uid?.let { db.collection("users").document(it).get().await() }
    val currentCoins = (userDoc?.getDouble("coins") ?: userDoc?.getLong("coins")?.toDouble()
    ?: userViewModel.coins.toDouble()).toLong()
    if (bet > currentCoins) {
        onResult(1.0, 0L); return
    }

    onScratch(true)

    val mult = rollMultiplier()
    if (mult * bet < bet) onImmediateLoss()

    delay(1200)

    val netApplied = applyCrashOutcomeAtomic(bet, mult)
    onScratch(false)

    if (netApplied == null) {
        onResult(1.0, 0L)
    } else {
        onResult(mult, netApplied)
    }
}


@Composable
private fun QuotePanel(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    val bg = if (positive) Color(0xFFE6FFF0) else Color(0xFFFFECEC)
    val stroke = if (positive) Color(0xFF37C077) else Color(0xFFE05757)
    val icon = if (positive) "🟢" else "🔴"
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(220),
        label = "quoteAlpha"
    )


    Surface(
        modifier = modifier.alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, stroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text, color = Color(0xFF1F2937), fontSize = 14.sp
            )
        }
    }
}


private suspend fun tryDeductCoinsAtomic(amount: Long): Boolean {
    if (amount <= 0L) return false
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return false
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val ref = db.collection("users").document(uid)
    return try {
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val coins =
                (snap.getDouble("coins") ?: snap.getLong("coins")?.toDouble() ?: 0.0).toLong()
            if (coins < amount) throw IllegalStateException("insufficient")
            tx.update(ref, "coins", coins - amount)
            null
        }.await()
        true
    } catch (_: Exception) {
        false
    }
}


fun preloadSound(context: Context, fileName: String): MediaPlayer? {
    return try {
        val assetFileDescriptor = context.assets.openFd(fileName)
        MediaPlayer().apply {
            setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            prepare()
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

@Composable
fun ZestyTicker(
    lines: List<String>,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF0E1320),
    bg: Color = Color(0xFFEAF0FF),
    speedPxPerSec: Float = 60f,
    gapDp: Dp = 48.dp,
    visible: Boolean = true
) {
    if (!visible || lines.isEmpty()) return

    val density = LocalDensity.current
    val gapPx = with(density) { gapDp.toPx() }
    rememberCoroutineScope()
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }

    val content = remember(lines) { lines.joinToString(separator = "   •   ") }
    val loopContent = remember(content) { "$content   •   $content   •   $content" }

    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Box(
            modifier = modifier
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp)
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { width = it.size.width }) {
                LaunchedEffect(loopContent, width, speedPxPerSec) {
                    if (width == 0) return@LaunchedEffect
                    while (true) {
                        val frame = withFrameNanos { it }
                        val start = frame
                        val pxPerNs = speedPxPerSec / 1_000_000_000f
                        while (true) {
                            val now = withFrameNanos { it }
                            val dt = (now - start).coerceAtMost(16_000_000)
                            offset -= dt * pxPerNs
                            if (kotlin.math.abs(offset) > width + gapPx) {
                                offset += width + gapPx
                            }
                            break
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TickerText(loopContent, offset, textColor)
                }
            }
        }
    }
}

@Composable
private fun TickerText(text: String, offset: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.graphicsLayer { translationX = offset })
    }
}