package com.nc.bangingbulls.Splash

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController) {

    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing)
                )
            )
        }

        delay(1000)
        val auth = FirebaseAuth.getInstance()
        val isUserLoggedIn = auth.currentUser != null


        Log.d("SplashScreen", "User logged in: $isUserLoggedIn")

        if (isUserLoggedIn) {
            navController.navigate("HomeScreen") {
                popUpTo("SplashScreen") { inclusive = true }
            }
        } else {
            navController.navigate("AuthScreen") {
                popUpTo("SplashScreen") { inclusive = true }
            }
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000)
        )
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⚫",
            fontSize = 64.sp,
            modifier = Modifier
                .rotate(rotation.value)
                .align(Alignment.TopCenter)
        )


        Text(
            text = " Loading...",
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            color = Color.Gray
        )

        LinearProgressIndicator(
            progress = progress.value,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 32.dp)
                .fillMaxWidth(0.7f),
            color = Color.Black
        )

    }
}