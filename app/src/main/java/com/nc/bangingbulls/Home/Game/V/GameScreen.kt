package com.nc.bangingbulls.Home.Game.V

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.R

@Composable
fun GameScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenW = configuration.screenWidthDp.dp
    val horizontalPad = (screenW * 0.04f).coerceAtLeast(12.dp)
    val corner = 16.dp
    val totalCoins = userViewModel.coins
    var pressedRoute by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020)) // fallback background color
    ) {
        // Full-screen background image
        Image(
            painter = painterResource(id = R.drawable.gamebg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Scrollable game content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPad, vertical = 16.dp)
        ) {
            Spacer(Modifier.height(60.dp)) // leave space for back button overlay

            // Coins display
            Surface(
                color = Color(0xFF0E1320),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Coins: 💰$totalCoins",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Pick your way to Gamble",
                color = Color(0xFFBFD1FF),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(12.dp))

            // Game cards
            GameCard(
                title = "Crash / Rocket",
                subtitle = "Scratch & reveal multipliers",
                route = "crashGame",
                pressed = pressedRoute == "crashGame",
                onClick = {
                    pressedRoute = "crashGame"
                    navController.navigate("crashGame")
                },
                accent = Color(0xFF3B68F7),
                corner = corner
            )
            Spacer(Modifier.height(6.dp))

            GameCard(
                title = "Dice / Hi-Lo",
                subtitle = "Predict higher or lower",
                route = "diceGame",
                pressed = pressedRoute == "diceGame",
                onClick = {
                    pressedRoute = "diceGame"
                    navController.navigate("diceGame")
                },
                accent = Color(0xFF7D56FF),
                corner = corner
            )
            Spacer(Modifier.height(6.dp))

            GameCard(
                title = "Limbo",
                subtitle = "Pick a target, dodge the crash",
                route = "limboGame",
                pressed = pressedRoute == "limboGame",
                onClick = {
                    pressedRoute = "limboGame"
                    navController.navigate("limboGame")
                },
                accent = Color(0xFF22C55E),
                corner = corner
            )
            Spacer(Modifier.height(6.dp))

            GameCard(
                title = "Coin Flip",
                subtitle = "Heads or tails—clean odds",
                route = "coinFlipGame",
                pressed = pressedRoute == "coinFlipGame",
                onClick = {
                    pressedRoute = "coinFlipGame"
                    navController.navigate("coinFlipGame")
                },
                accent = Color(0xFFFF6B6B),
                corner = corner
            )

            Spacer(Modifier.height(24.dp))
        }

        // Floating Back Button
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .clickable { navController.popBackStack() }
        )
    }
}

@Composable
private fun GameCard(
    title: String,
    subtitle: String,
    route: String,
    pressed: Boolean,
    onClick: () -> Unit,
    accent: Color,
    corner: Dp
) {
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable { onClick() },
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1320)),
        elevation = CardDefaults.cardElevation(4.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        // Accent strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accent.copy(alpha = 0.9f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent dot
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎮", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Color(0xFF8FA3BF), fontSize = 13.sp)
            }
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFFBFD1FF),
                modifier = Modifier.graphicsLayer { rotationZ = 180f } // right arrow
            )
        }
    }
}
