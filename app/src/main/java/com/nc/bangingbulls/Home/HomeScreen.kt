package com.nc.bangingbulls.Home

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.nc.bangingbulls.Home.Game.V.*
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.StocksRepository
import com.nc.bangingbulls.Home.Stocks.StockFiles.V.*
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

    val isMenuOpen = remember { mutableStateOf(false) }


    val navControllerHome = rememberNavController()
    val stocksViewModel = StocksViewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // immersive mode
    LaunchedEffect(Unit) {
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            } else {
                @Suppress("DEPRECATION") window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        }
    }

    val currentRoute = navControllerHome.currentBackStackEntryAsState().value?.destination?.route
    val hideChrome = currentRoute in ImmersiveRoutes

    Row(Modifier.fillMaxSize()) {
        // main content
        Box(Modifier.weight(1f)) {
            if (!hideChrome) {
                Scaffold(
                    topBar = {
                        TopBarWithImage(
                            imageRes = R.drawable.gamebg,
                            onMenu = { isMenuOpen.value = true },     // changed
                            onProfile = { navControllerHome.navigate("UserProfile") },
                        )
                    }
,
                            bottomBar = {
                        BottomBarWithImage(
                            imageRes = R.drawable.bottom,
                            items = items,
                            currentRoute = navControllerHome.currentBackStackEntryAsState().value?.destination?.route,
                            onItemClick = { item ->
                                navControllerHome.navigate(item.route) {
                                    popUpTo(navControllerHome.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    },
                    containerColor = Color.Transparent
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

        // right drawer
        if (drawerState.isOpen) {
            Box(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                        modifier = Modifier.clickable {
                            scope.launch { drawerState.close() }
                            authViewModel.signOut()
                            navController.navigate("AuthScreen") { popUpTo(0) { inclusive = true } }
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    if (isMenuOpen.value) {
        MenuOverlay(
            onDismiss = { isMenuOpen.value = false },
            onSignOut = {
                authViewModel.signOut()
                navController.navigate("AuthScreen") { popUpTo(0) { inclusive = true } }
            }
        )
    }



}
@Composable
fun MenuOverlay(
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    androidx.activity.compose.BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Menu", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onSignOut()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out", color = Color.White)
                }
            }
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
    val stocksRepository = remember { StocksRepository() }
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
                stocksViewModel = stocksViewModel,
                stocksRepository = stocksRepository,
                userId = userViewModel.auth.currentUser?.uid // <- now nullable
            )

        }
        composable(BottomNavItem.Stocks.route) { StocksScreen(navControllerHome, stocksViewModel) }
        composable(BottomNavItem.Game.route) { GameScreen(navControllerHome, userViewModel) }

        composable(
            route = "stock/{stockId}",
            arguments = listOf(navArgument("stockId") { type = NavType.StringType })
        ) { backStackEntry ->
            val stockId = backStackEntry.arguments?.getString("stockId") ?: ""
            StockDetailScreen(stockId, navControllerHome, stocksViewModel, userViewModel)
        }

        composable("AdminStockScreen") { AdminStockScreen(stocksViewModel, navControllerHome) }
        composable("About") { AboutScreen() }
        composable("TC") { TCScreen() }
        composable("Settings") { Settings(navControllerHome) }

        // ADD THIS ROUTE
        composable("UserProfile") {
            UserProfileScreen(
                navController = navControllerHome,
                userViewModel = userViewModel
            )
        }

        immersiveGameRoute("crashGame") { CrashGameScreen(userViewModel) { navControllerHome.popBackStack() } }
        immersiveGameRoute("diceGame") { DiceGameScreen(userViewModel) { navControllerHome.popBackStack() } }
        immersiveGameRoute("limboGame") { LimboGameScreen(navControllerHome, userViewModel) }
        immersiveGameRoute("coinFlipGame") { CoinFlipGameScreen(navControllerHome, userViewModel) }
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
    onProfile: (() -> Unit)? = null,
    userName: String? = null,
    userEmail: String? = null,
    coins: Long? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = WindowInsets.statusBars.asPaddingValues()
) {
    val topBarHeight = 56.dp
    var showProfileCard by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topBarHeight + contentPadding.calculateTopPadding())
            .background(Color.Transparent)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.22f)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = contentPadding.calculateTopPadding())
                .height(topBarHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { showProfileCard = true },
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.face),
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.weight(1f))

            if (coins != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "💰 $coins",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
            }

            IconButton(onClick = onMenu) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }
        }

        DropdownMenu(
            expanded = showProfileCard,
            onDismissRequest = { showProfileCard = false },
            modifier = Modifier
                .widthIn(min = 220.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.face),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(userName ?: "User", fontWeight = FontWeight.SemiBold, color = Color(0xFF0E1320))
                        if (!userEmail.isNullOrBlank()) {
                            Text(userEmail, color = Color(0xFF667085), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                if (coins != null) {
                    Text("Coins: $coins", color = Color(0xFF0E1320), fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    "View profile",
                    color = Color(0xFF3B68F7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showProfileCard = false
                            onProfile?.invoke()
                        }
                        .padding(vertical = 4.dp),
                    fontSize = 13.sp
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
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
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
