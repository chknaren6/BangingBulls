package com.nc.bangingbulls.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nc.bangingbulls.Authentication.AuthViewModel
import com.nc.bangingbulls.Home.Game.GameScreen
import com.nc.bangingbulls.Home.Stocks.AdminStockScreen
import com.nc.bangingbulls.Home.Stocks.StockDetailScreen
import com.nc.bangingbulls.Home.Stocks.StocksScreen
import com.nc.bangingbulls.stocks.StocksViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, authViewModel: AuthViewModel, userViewModel: UserViewModel) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Stocks,
        BottomNavItem.Game
    )
    val navControllerHome = rememberNavController()
    val stocksViewModel = StocksViewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userViewModel.loadUserData()
    }
    LaunchedEffect(Unit) {
        userViewModel.loadHoldings()
    }
    LaunchedEffect(Unit) {
        userViewModel.loadLeaderboard()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface) // solid color
                    .padding(16.dp)
            ) {
                Text("About", modifier = Modifier.clickable { navControllerHome.navigate("About") })
                Spacer(Modifier.height(8.dp))
                Text("Terms & Conditions", modifier = Modifier.clickable { navControllerHome.navigate("TC") })
                Spacer(Modifier.height(8.dp))
                Text("Settings", modifier = Modifier.clickable {
                    scope.launch { drawerState.close() }
                    navControllerHome.navigate("Settings")
                })
                Spacer(Modifier.height(24.dp))
                Text(
                    "Sign Out",
                    modifier = Modifier
                        .clickable {
                            scope.launch { drawerState.close() }
                            authViewModel.signOut()
                            navController.navigate("AuthScreen") { popUpTo(0) { inclusive = true } }
                        },
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
       // drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) // optional opacity
    )

    {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
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
        ) { padding ->
            NavHost(
                navController = navControllerHome,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreenContent(
                        userViewModel,
                        authViewModel,
                        navController,
                        navControllerHome,
                        stocksViewModel,
                       // modifier = Modifier.padding(top = 16.dp) // add extra top padding for coins animation
                    )
                }
                composable(BottomNavItem.Stocks.route) {
                    StocksScreen(navControllerHome, stocksViewModel)
                }
                composable(BottomNavItem.Game.route) {
                    GameScreen(userViewModel)
                }

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

                    composable("About"){
                        AboutScreen()
                    }
                    composable("TC"){
                        TCScreen()
                    }
                    composable("Settings"){
                        Settings(navControllerHome)
                    }


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




