package com.nc.bangingbulls.Splash

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.nc.bangingbulls.Authentication.FastCircularProgressIndicator
import com.nc.bangingbulls.Home.Stocks.Leaderboard.LeaderboardWorker
import java.util.concurrent.TimeUnit

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val rotation = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    // Schedule WorkManager once
    LaunchedEffect(Unit) {
        val request = PeriodicWorkRequestBuilder<LeaderboardWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "leaderboard_refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    // Navigation
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val isUserLoggedIn = auth.currentUser != null
        Log.d("SplashScreen", "User logged in: $isUserLoggedIn")
        navController.navigate(if (isUserLoggedIn) "HomeScreen" else "AuthScreen") {
            popUpTo("SplashScreen") { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        FastCircularProgressIndicator(modifier = Modifier)
        Text(
            text = "⚫",
            fontSize = 64.sp,
            modifier = Modifier.rotate(rotation.value).align(Alignment.TopCenter)
        )
        Text(
            text = " Loading...",
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            color = Color.Gray
        )
        LinearProgressIndicator(
            progress = progress.value,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 32.dp).fillMaxWidth(0.7f),
            color = Color.Black
        )
    }
}
