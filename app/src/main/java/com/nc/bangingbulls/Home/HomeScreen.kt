package com.nc.bangingbulls.Home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.nc.bangingbulls.Authentication.AuthViewModel

@Composable
fun HomeScreen( navController: NavController,AuthViewModel: AuthViewModel) {

    Column(Modifier.fillMaxSize()) {
        Text("HomeScreen")
        Button(onClick = { AuthViewModel.signOut()
            navController.navigate("AuthScreen")}) {
         Text("SignOut")
        }
    }

}