package com.nc.bangingbulls.Authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
 /*   var user by mutableStateOf<FirebaseUser?>(null)
        private set

    var isNewUser by mutableStateOf(false)
        private set

    var username by mutableStateOf("")
    var email by mutableStateOf("")

    fun setUser(firebaseUser: FirebaseUser?) {
        user = firebaseUser
        email = firebaseUser?.email.orEmpty()
    }
*/
    fun signOut() {
        Firebase.auth.signOut()
        //user = null
    }

    fun checkIfNewUser(userId: String, onResult: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                onResult(!doc.exists())
            }
    }

    fun addUserToFirestore(userId: String, userData: Map<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).set(userData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
