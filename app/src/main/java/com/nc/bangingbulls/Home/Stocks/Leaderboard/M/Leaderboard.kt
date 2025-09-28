package com.nc.bangingbulls.Home.Stocks.Leaderboard.M


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LeaderboardWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val repo =
                LeaderboardRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())
            repo.recomputeForCurrentUser()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}