package com.nc.bangingbulls.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Authentication.V.AuthScreen
import com.nc.bangingbulls.Authentication.VM.AuthViewModel
import com.nc.bangingbulls.Authentication.VM.AuthViewModelFactory
import com.nc.bangingbulls.Authentication.V.UsernameInputScreen
import com.nc.bangingbulls.Home.HomeScreen
import com.nc.bangingbulls.Home.UserViewModel
import com.nc.bangingbulls.Splash.SplashScreen

@Composable
fun Navi(navController: NavHostController) {
    LocalContext.current

    val factory = AuthViewModelFactory(
        FirebaseFirestore.getInstance(),
        FirebaseAuth.getInstance()
    )
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val userViewModel: UserViewModel = viewModel()
    NavHost(navController = navController, startDestination = "SplashScreen") {
        composable("SplashScreen") {
            SplashScreen(navController)
        }
        composable("AuthScreen") {
            AuthScreen(navController)
        }
        composable("HomeScreen") {
            HomeScreen(navController,authViewModel, userViewModel)
        }
        composable("UserNameInputScreen") {
            UsernameInputScreen(navController,authViewModel)
        }
    }
}
