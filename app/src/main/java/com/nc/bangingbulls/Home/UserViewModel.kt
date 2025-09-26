package com.nc.bangingbulls.Home

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.nc.bangingbulls.Home.User

class UserViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var username by mutableStateOf("")
    var coins by mutableStateOf(0)
    var profileUrl by mutableStateOf<String?>(null)

    fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    username = snapshot.getString("username") ?: ""
                    coins = snapshot.getLong("coins")?.toInt() ?: 0
                    profileUrl = snapshot.getString("profileUrl")
                }
            }
    }

    fun updateCoins(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("coins", FieldValue.increment(amount.toLong()))
    }
}


