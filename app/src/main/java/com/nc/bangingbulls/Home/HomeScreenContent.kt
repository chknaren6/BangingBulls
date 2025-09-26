package com.nc.bangingbulls.Home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nc.bangingbulls.Authentication.AuthViewModel


@Composable
fun HomeScreenContent(userViewModel: UserViewModel, authViewModel: AuthViewModel,navController: NavController) {
    val coins by remember{ derivedStateOf { userViewModel.coins } }
    val animatedCoins by animateIntAsState(targetValue = coins, animationSpec = tween(600))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Welcome, ${userViewModel.username}!", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Text("💰 $animatedCoins", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            Button(onClick = {authViewModel.signOut()
            navController.navigate("AuthScreen"){
                popUpTo("HomeScreen"){inclusive = true}
            } }) {
                Text("Sign Out")
            }

        }

        Spacer(Modifier.height(24.dp))

        // Profile
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            if (userViewModel.profileUrl != null) {
               /* Image(
                    painter = rememberAsyncImagePainter(userViewModel.profileImageUri),
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )*/
            } else {
                Icon(Icons.Default.Person, contentDescription = "Default Profile", modifier = Modifier.fillMaxSize())
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Popular Stocks", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyRow {
            items(listOf("PornHub", "Brazzers", "XNNX", "Xvideos")) { stock ->
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .width(150.dp)
                        .height(100.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(stock, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}