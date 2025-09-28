package com.nc.bangingbulls.Home

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.nc.bangingbulls.Authentication.VM.AuthViewModel
import com.nc.bangingbulls.Home.Game.V.CoinFlipGameScreen
import com.nc.bangingbulls.Home.Game.V.CrashGameScreen
import com.nc.bangingbulls.Home.Game.V.DiceGameScreen
import com.nc.bangingbulls.Home.Game.V.GameScreen
import com.nc.bangingbulls.Home.Game.V.LimboGameScreen
import com.nc.bangingbulls.Home.Stocks.StockFiles.V.AdminStockScreen
import com.nc.bangingbulls.Home.Stocks.StockFiles.V.StockDetailScreen
import com.nc.bangingbulls.Home.Stocks.StockFiles.V.StocksScreen
import com.nc.bangingbulls.Home.Stocks.StockFiles.VM.StocksViewModel
import com.nc.bangingbulls.R
import kotlinx.coroutines.launch

private val ImmersiveRoutes = setOf("crashGame", "diceGame", "limboGame", "coinFlipGame")

@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    navController: NavController, authViewModel: AuthViewModel, userViewModel: UserViewModel
) {
    val items = listOf(BottomNavItem.Home, BottomNavItem.Stocks, BottomNavItem.Game)

    LaunchedEffect(Unit) {
        userViewModel.claimDailyCoins()
        userViewModel.loadUserData()
        userViewModel.loadHoldings()
        userViewModel.loadLeaderboard()
    }


    val navControllerHome = rememberNavController()
    val stocksViewModel = StocksViewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val activity = context as? ComponentActivity


    LaunchedEffect(Unit) {
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            } else {
                @Suppress("DEPRECATION") window.decorView.systemUiVisibility =
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState, drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text("About", modifier = Modifier.clickable { navControllerHome.navigate("About") })
                Spacer(Modifier.height(8.dp))
                Text(
                    "Terms & Conditions",
                    modifier = Modifier.clickable { navControllerHome.navigate("TC") })
                Spacer(Modifier.height(8.dp))
                Text("Settings", modifier = Modifier.clickable {
                    scope.launch { drawerState.close() }
                    navControllerHome.navigate("Settings")
                })
                Spacer(Modifier.height(24.dp))
                Text(
                    "Sign Out", modifier = Modifier.clickable {
                        scope.launch { drawerState.close() }
                        authViewModel.signOut()
                        navController.navigate("AuthScreen") { popUpTo(0) { inclusive = true } }
                    }, color = MaterialTheme.colorScheme.error
                )
            }
        }) {
        val currentRoute =
            navControllerHome.currentBackStackEntryAsState().value?.destination?.route
        val hideChrome = currentRoute in ImmersiveRoutes
        if (!hideChrome) {
            Scaffold(
                topBar = {
                TopBarWithImage(
                    imageRes = R.drawable.gamebg,
                    onMenu = { scope.launch { drawerState.open() } })
            }, bottomBar = {
                BottomBarWithImage(
                    imageRes = R.drawable.bottom,
                    items = items,
                    currentRoute = navControllerHome.currentBackStackEntryAsState().value?.destination?.route,
                    onItemClick = { item ->
                        navControllerHome.navigate(item.route) {
                            popUpTo(navControllerHome.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    })
            }, containerColor = Color.Transparent
            ) { padding ->
                HomeNavHost(
                    navControllerHome = navControllerHome,
                    userViewModel = userViewModel,
                    stocksViewModel = stocksViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

        } else {
            HomeNavHost(
                navControllerHome = navControllerHome,
                userViewModel = userViewModel,
                stocksViewModel = stocksViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeNavHost(
    navControllerHome: NavController,
    userViewModel: UserViewModel,
    stocksViewModel: StocksViewModel,
    modifier: Modifier
) {
    AnimatedNavHost(
        navController = navControllerHome as NavHostController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreenContent(
                userViewModel = userViewModel,
                authViewModel = null,
                navController = navControllerHome,
                navControllerHome = navControllerHome,
                stocksViewModel = stocksViewModel
            )
        }
        composable(BottomNavItem.Stocks.route) {
            StocksScreen(navControllerHome, stocksViewModel)
        }
        composable(BottomNavItem.Game.route) {
            GameScreen(navControllerHome, userViewModel)
        }

        composable(
            route = "stock/{stockId}",
            arguments = listOf(navArgument("stockId") { type = NavType.StringType })
        ) { backStackEntry ->
            val stockId = backStackEntry.arguments?.getString("stockId") ?: ""
            StockDetailScreen(stockId, navControllerHome, stocksViewModel)
        }

        composable("AdminStockScreen") { AdminStockScreen(stocksViewModel, navControllerHome) }
        composable("About") { AboutScreen() }
        composable("TC") { TCScreen() }
        composable("Settings") { Settings(navControllerHome) }

        immersiveGameRoute("crashGame") {
            CrashGameScreen(userViewModel) { navControllerHome.popBackStack() }
        }
        immersiveGameRoute("diceGame") {
            DiceGameScreen(userViewModel, { navControllerHome.popBackStack() })
        }
        immersiveGameRoute("limboGame") {
            LimboGameScreen(navControllerHome, userViewModel)
        }
        immersiveGameRoute("coinFlipGame") {
            CoinFlipGameScreen(navControllerHome, userViewModel)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.immersiveGameRoute(
    route: String, content: @Composable () -> Unit
) {
    composable(route = route, enterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400))
    }, exitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400))
    }, popEnterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400))
    }, popExitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400))
    }) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}

@Composable
fun TopBarWithImage(
    imageRes: Int,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = WindowInsets.statusBars.asPaddingValues()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp + contentPadding.calculateTopPadding())
            .background(Color.Transparent)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(RectangleShape)
        )

        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = contentPadding.calculateTopPadding())
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.width(1.dp))
            IconButton(onClick = onMenu) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun BottomBarWithImage(
    imageRes: Int,
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = WindowInsets.navigationBars.asPaddingValues()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp + contentPadding.calculateBottomPadding())
            .background(Color.Transparent)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = contentPadding.calculateBottomPadding())
                .height(64.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onItemClick(item) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        item.label,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

