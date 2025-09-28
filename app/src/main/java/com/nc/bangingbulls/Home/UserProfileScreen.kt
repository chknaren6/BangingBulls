package com.nc.bangingbulls.Home


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.R

@Composable
fun UserProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val username by remember { derivedStateOf { userViewModel.username } }
    val coins by remember { derivedStateOf { userViewModel.coins } }
    val lifetime by remember { mutableStateOf<Long?>(null) }
    val email = userViewModel.auth.currentUser?.email ?: ""
    var editing by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(username) }


    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.homebg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text("Profile", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Image(
                        painter = painterResource(R.drawable.face),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!editing) {
                            Text(
                                text = username.ifBlank { "User" },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = {
                                editing = true
                                nameDraft = username
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                            }
                        } else {
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it },
                                singleLine = true,
                                label = { Text("Username") }
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                userViewModel.updateUsername(nameDraft.trim())
                                editing = false
                            }) { Text("Save") }
                        }
                    }
                    Text(email, color = Color(0xFFCBD5E1), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1320)),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatPill(label = "Coins", value = coins.toString())
                    StatPill(label = "Lifetime", value = (lifetime ?: 0L).toString())
                }
            }

            Spacer(Modifier.height(16.dp))

            // Actions
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f))
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Quick Actions", fontWeight = FontWeight.SemiBold, color = Color(0xFF0E1320))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProfileButton("Claim daily") { userViewModel.claimDailyCoins() }
                        ProfileButton("Settings") { navController.navigate("Settings") }
                        ProfileButton("Sign out") {
                            // Navigate out via HomeScreen’s sign-out if needed
                            navController.navigate("AuthScreen") { popUpTo(0) { inclusive = true } }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("About this account", fontWeight = FontWeight.SemiBold, color = Color(0xFF0E1320))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Track coins, daily rewards, and gameplay limits here. Username changes update instantly.",
                        color = Color(0xFF334155)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFFBFD1FF), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Surface(
            color = Color.Black.copy(alpha = 0.35f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                value,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B68F7))
    ) {
        Text(text, color = Color.White)
    }
}
