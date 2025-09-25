package com.nc.bangingbulls.Authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.User

class AuthViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var navigateToHome by mutableStateOf(false)
        private set
    var userLoaded by mutableStateOf(false)
        private set
    var uName by mutableStateOf<String?>(null)
        private set
    var isNewUser by mutableStateOf(false)
        private set
    var currentUser by mutableStateOf<User?>(null)
        private set

    fun addUserToFirestore(
        uid: String,
        username: String,
        email: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        isLoading = true
        errorMessage = null

        val user = User(
            uid = uid,
            username = username,
            email = email,
            coins = 2765,
            spentCoins = 0,
            lostCoins = 0,
            lifeTimeEarnings = 0,
            lastRewardTimestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            profileStatus = "active"
        )

        firestore.collection("users").document(uid).set(user).addOnSuccessListener {
                uName = username
                currentUser = user
                userLoaded = true
                isNewUser = false
                isLoading = false
                navigateToHome = true
                onSuccess()
            }.addOnFailureListener { ex ->
                isLoading = false
                errorMessage = ex.localizedMessage
                onFailure(ex)
            }
    }

    fun handleSignIn(uid: String, email: String, onNewUser: () -> Unit, onExistingUser: () -> Unit) {
        isLoading = true
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                isLoading = false
                if (doc.exists()) {
                    currentUser = doc.toObject(User::class.java)
                    userLoaded = true
                    navigateToHome = true
                    onExistingUser()
                } else {
                    isNewUser = true
                    onNewUser()
                }
            }
            .addOnFailureListener { ex ->
                isLoading = false
                errorMessage = ex.localizedMessage
            }
    }

    fun resetState() {
        isLoading = false
        errorMessage = null
        uName = null
        userLoaded = false
        navigateToHome = false
        isNewUser = false
        currentUser = null
    }

    fun signOut() {
        auth.signOut()
        resetState()
    }

    fun clearNavigationFlag() {
        navigateToHome = false
    }

    fun setUserName(name: String?) {
        uName = name
    }

    fun setError(msg: String?) {
        errorMessage = msg
    }
}
