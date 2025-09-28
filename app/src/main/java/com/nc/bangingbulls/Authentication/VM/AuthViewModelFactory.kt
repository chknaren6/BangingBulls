package com.nc.bangingbulls.Authentication.VM

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModelFactory(
    private val firestore: FirebaseFirestore, private val auth: FirebaseAuth
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return AuthViewModel(firestore, auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}