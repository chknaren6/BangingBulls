package com.nc.bangingbulls.Home

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private var userCache: List<User> = emptyList()

    fun getUsersFromCache(): List<User> = userCache

    suspend fun fetchAllUsers(): List<User> {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("users").get().await()
        userCache = snapshot.documents.map { doc ->
            User(doc.id, doc.getString("username") ?: "", doc.getString("email") ?: "")
        }
        return userCache
    }
}
