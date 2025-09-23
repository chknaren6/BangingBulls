package com.nc.bangingbulls.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nc.bangingbulls.Authentication.AuthScreen
import com.nc.bangingbulls.Home.HomeScreen
import com.nc.bangingbulls.Splash.SplashScreen

@Composable
fun Navi(navController: NavHostController) {
    LocalContext.current

    NavHost(navController = navController, startDestination = "SplashScreen") {
        composable("SplashScreen") {
            SplashScreen(navController)
        }
        composable("AuthScreen") {
            AuthScreen(navController)
        }
        composable("HomeScreen") {
            HomeScreen(navController)
        }
    }
}
