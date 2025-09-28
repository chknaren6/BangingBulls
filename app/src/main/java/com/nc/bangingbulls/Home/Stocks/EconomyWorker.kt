package com.nc.bangingbulls.Home.Stocks


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.stocks.StocksRepository

class EconomyTickWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val repo = StocksRepository(FirebaseFirestore.getInstance())
            repo.tickEconomy()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

class NightlyArchiveWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val repo = StocksRepository(FirebaseFirestore.getInstance())
            repo.archiveTodayToLastWeek()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
