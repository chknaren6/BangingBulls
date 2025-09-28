package com.nc.bangingbulls.Home.Stocks
data class User(
    val uid: String,
    val email: String,
    val username: String,
    val coins: Int = 2765,
    val spentCoins: Int = 0,
    val lostCoins: Int = 0,
    val lifeTimeEarnings: Int = 0,
    val lastRewardTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val profileStatus: String = "active",
    val isAdmin: Boolean = false,
)
