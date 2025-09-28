package com.nc.bangingbulls.Authentication.V


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.R

@Composable
fun IntroCardScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.homebg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x990B1020))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Welcome to Banging Bulls",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0E1320)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        // Two-line crisp game idea
                        "Play fast mini‑games to win coins.\nBet smart, celebrate wins, and keep limits.",
                        fontSize = 14.sp,
                        color = Color(0xFF334155),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(18.dp))

                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            if (uid == null) {
                                navController.navigate("AuthScreen") { popUpTo(0) { inclusive = true } }
                                return@Button
                            }
                            saving = true
                            FirebaseFirestore.getInstance()
                                .collection("users").document(uid)
                                .update("introSeen", true)
                                .addOnSuccessListener {
                                    saving = false
                                    navController.navigate("HomeScreen") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                .addOnFailureListener {
                                    saving = false
                                    error = it.localizedMessage
                                }
                        },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B68F7))
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
