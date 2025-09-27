package com.nc.bangingbulls.Home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Authentication.AuthViewModel
import com.nc.bangingbulls.Home.Stocks.AdminStockScreen
import com.nc.bangingbulls.Home.Stocks.StockDetailScreen
import com.nc.bangingbulls.Home.Stocks.StocksScreen
import com.nc.bangingbulls.stocks.StocksViewModel

@Composable
fun HomeScreen(navController: NavController, authViewModel: AuthViewModel, userViewModel: UserViewModel) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Stocks,
        BottomNavItem.Game
    )
    val navControllerHome = rememberNavController()
    val stocksViewModel = StocksViewModel()


    LaunchedEffect(Unit) {
        userViewModel.loadUserData()
    }
    LaunchedEffect(Unit) {
        userViewModel.loadHoldings()
    }
    LaunchedEffect(Unit) {
        userViewModel.loadLeaderboard()
    }


    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navControllerHome.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navControllerHome.navigate(item.route) {
                                popUpTo(navControllerHome.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navControllerHome,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home tab
            composable(BottomNavItem.Home.route) {
                HomeScreenContent(userViewModel, authViewModel, navControllerHome, stocksViewModel)
            }


            // Stocks tab (list view)
            composable(BottomNavItem.Stocks.route) {
                StocksScreen(navControllerHome, stocksViewModel)
            }

            // Game tab
            composable(BottomNavItem.Game.route) {
                GameScreen(userViewModel)
            }

            // Stock detail screen (nested navigation from Stocks tab)
            composable(
                route = "stock/{stockId}",
                arguments = listOf(navArgument("stockId") { type = NavType.StringType })
            ) { backStackEntry ->
                val stockId = backStackEntry.arguments?.getString("stockId") ?: ""
                StockDetailScreen(
                    stockId = stockId,
                    navController = navControllerHome,
                    stocksViewModel = stocksViewModel
                )
            }

            composable("AdminStockScreen"){
                AdminStockScreen(stocksViewModel,navControllerHome)
            }


        }
    }
}



fun generateHistory(startPrice: Double, days: Int): List<Map<String, Any>> {
    val history = mutableListOf<Map<String, Any>>()
    var currentPrice = startPrice
    val now = System.currentTimeMillis() / 1000  // Unix timestamp (sec)

    for (i in days downTo 1) {
        currentPrice += (-3..3).random() * 1.5  // random small change
        val timestamp = now - (i * 86400)       // subtract i days
        history.add(mapOf("time" to timestamp, "price" to currentPrice))
    }
    return history
}

// Usage:



