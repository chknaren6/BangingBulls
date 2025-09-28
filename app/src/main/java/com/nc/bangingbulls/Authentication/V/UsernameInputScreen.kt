package com.nc.bangingbulls.Authentication.V

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import kotlinx.coroutines.delay

@Composable
fun UsernameInputScreen(
    navController: NavController, viewModel: AuthViewModel
) {
    var isCardVisible by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val error = viewModel.errorMessage
    val isLoading = viewModel.isLoading
    val navigateToHome = viewModel.navigateToHome
    val firebaseUser = Firebase.auth.currentUser

    LaunchedEffect(Unit) {
        delay(300L)
        isCardVisible = true
    }

    LaunchedEffect(navigateToHome) {
        if (navigateToHome) {
            viewModel.clearNavigationFlag()
            navController.navigate("HomeScreen") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        /*Image(
            painter = painterResource(
                id = R.drawable.face
            ),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )*/

        AnimatedVisibility(
            visible = isCardVisible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                initialOffsetY = { it }) + fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(400))) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2F2F2F))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Alright, Rockstar!",
                        fontSize = 28.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { input ->
                            val cleanedInput =
                                input.replace("[^a-zA-Z0-9]".toRegex(), "").uppercase().take(10)

                            username = cleanedInput
                            viewModel.setUserName(cleanedInput)
                        },

                        label = { Text("Enter your in-game name", color = Color(0xFF5E4040)) },
                        singleLine = true,
                        isError = !error.isNullOrBlank(),
                        supportingText = {
                            if (!error.isNullOrBlank()) {
                                Text(error ?: "", color = Color(0xFFFFD700), fontSize = 12.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFAFAFAF),
                            unfocusedBorderColor = Color.White,
                            cursorColor = Color(0xFFCDDC39),
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                        ),
                        textStyle = TextStyle(color = Color.White),
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            hasAttemptedSubmit = true
                            val name = username.trim()

                            when {
                                name.isBlank() -> viewModel.setError("Don't be shy now…")
                                name.length < 5 -> viewModel.setError("Short name, big shame! At least 5+ chars")
                                name.length > 10 -> viewModel.setError("Keep it 5-10 characters")
                                firebaseUser != null -> {
                                    viewModel.addUserToFirestore(
                                        uid = firebaseUser.uid,
                                        username = name,
                                        email = firebaseUser.email ?: "",
                                        onSuccess = {
                                            navController.navigate("HomeScreen")
                                        },
                                        onFailure = {
                                            viewModel.setError(it.localizedMessage)
                                        })
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D5B43))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Confirm", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
