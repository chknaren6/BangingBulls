package com.nc.bangingbulls.Splash

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.nc.bangingbulls.R
import com.nc.bangingbulls.Authentication.V.FastCircularProgressIndicator
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    val player by remember(context) { mutableStateOf(preloadSound(context, "intro.mp3")) }

    var audioEnded by remember { mutableStateOf(false) }
    var audioDurationMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        try {
            val dur = player?.duration ?: -1
            if (dur > 0) audioDurationMs = dur.toLong()
            playPreloaded(player)
        } catch (_: Exception) { audioEnded = true }
    }

    DisposableEffect(player) {
        player?.setOnCompletionListener {
            audioEnded = true
        }
        onDispose { player?.release() }
    }

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(1.06f, tween(700, easing = FastOutSlowInEasing))
            pulse.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
    }

    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(audioDurationMs) {
        val total = (audioDurationMs ?: 1200L).coerceAtLeast(600L)
        val steps = 20
        val stepMs = total / steps
        repeat(steps) {
            delay(stepMs)
            progress = (it + 1) / steps.toFloat()
        }
        audioEnded = true
    }

    // Navigate after audio finish + small fade
    var fadingOut by remember { mutableStateOf(false) }
    var navigated by remember { mutableStateOf(false) }
    LaunchedEffect(audioEnded) {
        if (audioEnded && !navigated) {
            fadingOut = true
            delay(150)
            navigated = true
            val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null
            navController.navigate(if (isUserLoggedIn) "HomeScreen" else "AuthScreen") {
                popUpTo("SplashScreen") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
    ) {
        Image(
            painter = painterResource(R.drawable.homebg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.18f),
            contentScale = ContentScale.Crop
        )

        AnimatedVisibility(
            visible = !fadingOut,
            exit = fadeOut(tween(140))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🟡",
                        fontSize = 64.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pulse.value
                            scaleY = pulse.value
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    com.nc.bangingbulls.Authentication.V.FastCircularProgressIndicator(
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.height(22.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        color = Color(0xFF3B68F7),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Loading...",
                        color = Color(0xFFBFD1FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


fun preloadSound(context: Context, fileName: String): MediaPlayer? {
    return try {
        val afd = context.assets.openFd(fileName)
        MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            prepare()
            setOnErrorListener { _, what, extra ->
                Log.e("Audio", "Splash audio error: what=$what extra=$extra")
                false
            }
        }.also { afd.close() }
    } catch (e: Exception) {
        Log.e("Audio", "Failed to preload $fileName: ${e.message}")
        null
    }
}

fun playPreloaded(player: MediaPlayer?) {
    if (player == null) return
    try {
        if (player.isPlaying) {
            player.stop()
            player.prepare()
        }
        player.seekTo(0)
        player.start()
    } catch (_: Exception) { }
}
