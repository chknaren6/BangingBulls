package com.nc.bangingbulls.Authentication.V

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.nc.bangingbulls.Authentication.VM.AuthViewModel
import com.nc.bangingbulls.R
import kotlinx.coroutines.delay
@Composable
fun UsernameInputScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    var isCardVisible by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }

    val error = viewModel.errorMessage
    val isLoading = viewModel.isLoading
    val navigateToHome = viewModel.navigateToHome
    val firebaseUser = com.google.firebase.Firebase.auth.currentUser

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(220L)
        isCardVisible = true
    }

    // Navigate once
    LaunchedEffect(navigateToHome) {
        if (navigateToHome) {
            viewModel.clearNavigationFlag()
            navController.navigate("HomeScreen") { popUpTo(0) { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.facee),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color(0x880B1020),
                        0.35f to Color.Transparent,
                        1f to Color(0x990B1020)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isCardVisible,
                enter = slideInVertically(
                    initialOffsetY = { full -> (full * 0.25f).toInt() },
                    animationSpec = tween(520, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(420)),
                exit = fadeOut(tween(300))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF21E1E1E)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 22.dp, vertical = 20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Alright, Rockstar!",
                            fontSize = 26.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { input ->
                                val cleaned = input
                                    .replace("[^a-zA-Z0-9]".toRegex(), "")
                                    .uppercase()
                                    .take(10)
                                username = cleaned
                                viewModel.setUserName(cleaned)
                            },
                            label = { Text("Enter your in‑game name", color = Color(0xFF93A1B2), fontSize = 13.sp) },
                            singleLine = true,
                            isError = !error.isNullOrBlank(),
                            supportingText = {
                                if (!error.isNullOrBlank()) {
                                    Text(
                                        error ?: "",
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Text("5–10 letters/numbers, UPPERCASE", color = Color(0xFF93A1B2), fontSize = 11.sp)
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B68F7),
                                unfocusedBorderColor = Color(0xFF6B7280),
                                cursorColor = Color(0xFFCDDC39),
                                focusedLabelColor = Color(0xFFBFD1FF),
                                unfocusedLabelColor = Color(0xFF93A1B2)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val name = username.trim()
                                when {
                                    name.isBlank() -> viewModel.setError("Don’t be shy now…")
                                    name.length < 5 -> viewModel.setError("At least 5 characters")
                                    name.length > 10 -> viewModel.setError("Keep it 5–10 characters")
                                    firebaseUser != null -> {
                                        viewModel.addUserToFirestore(
                                            uid = firebaseUser.uid,
                                            username = name,
                                            email = firebaseUser.email ?: "",
                                            onSuccess = {
                                                navController.navigate("HomeScreen") { popUpTo(0) { inclusive = true } }
                                            },
                                            onFailure = { viewModel.setError(it.localizedMessage) }
                                        )
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B68F7))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text("Confirm", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

