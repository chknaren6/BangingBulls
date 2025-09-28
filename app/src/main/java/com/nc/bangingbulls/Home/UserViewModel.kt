package com.nc.bangingbulls.Home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.Holding
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {
    private val db = Firebase.firestore
    val auth = Firebase.auth

    var isAdmin by mutableStateOf(false)
    var username by mutableStateOf("")
    var coins: Long by mutableStateOf(0)
    var profileUrl by mutableStateOf<String?>(null)
    var holdings by mutableStateOf<List<Holding>>(emptyList())
    var leaderboard by mutableStateOf<List<LeaderboardUser>>(emptyList())

    private val userRef
        get() = auth.currentUser?.uid?.let { db.collection("users").document(it) }

    init {
        loadUserData()
        loadHoldings()
        loadLeaderboard()
    }
    var lifetimeCoins by mutableStateOf(0L)



    val winLines = listOf(
        "You actually won? Guess even a broken clock’s right twice a day!",
        "Big win, huh? Don’t let it go to your head—it’s still tiny.",
        "Luck finally pitied you! Bet again before it changes its mind.",
        "You’re on fire? Nah, just a spark—keep betting, dimwit!",
        "A win? Miracle alert! Don’t screw it up with your next move.",
        "Fortune threw you a bone—don’t choke on it, champ.",
        "You pulled it off? Even a blind squirrel finds a nut sometimes.",
        "Nice one, genius! Bet bigger or stay a small-time fluke.",
        "The game let you win—don’t act like you’re some hotshot now.",
        "Victory? Cute. Keep spinning or crawl back to mediocrity.",
        "You’re gloating already? One win doesn’t make you less lame.",
        "Grabbed a win? Don’t trip over your ego, keep betting!",
        "Wow, you won! Even the game’s shocked at your dumb luck.",
        "A fluke win? Bet again or forever hold your pathetic peace.",
        "You’re up! Don’t ruin it by thinking you’re actually good."
    )

    val loseLines = listOf(
        "Lost again? Your wallet’s screaming for a better owner!",
        "Ouch, another flop! Your luck’s uglier than your last bet.",
        "You suck at this! Spin again or cry into your empty pockets.",
        "Lost? Shocker. Maybe bet with brains instead of wishes.",
        "Your losing streak’s so long it needs its own zip code!",
        "Down again? Even the game’s bored of your sad bets.",
        "You’re bleeding chips! Keep going or admit you’re cursed.",
        "Another loss? Your skill’s a myth, like your winning streak.",
        "Crashed and burned! Bet again or slink away, loser.",
        "Your bets are so bad, the game’s begging for mercy!",
        "Lost it all? No surprise—your strategy’s a total clown show.",
        "Keep losing like that, and you’ll owe the game an apology!",
        "Swing and a miss! Try again or stick to something easier, like napping.",
        "Your luck’s so trash, it’s practically a landfill! Bet anyway.",
        "Flopped again? At this rate, you’re the game’s favorite punching bag!"
    )



    fun loadUserData() {
        val ref = userRef ?: return
        ref.addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                username = snap.getString("username") ?: ""
                coins = (snap.getLong("coins") ?: 0L)
                lifetimeCoins = (snap.getLong("lifetimeCoins") ?: 0L)
                profileUrl = snap.getString("profileUrl")
                isAdmin = snap.getBoolean("admin") ?: false
            }
        }
    }


    fun loadHoldings() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("holdings")
            .addSnapshotListener { snap, _ ->
                holdings = snap?.documents?.mapNotNull { doc ->
                    val sid = doc.getString("stockId") ?: return@mapNotNull null
                    val qty = doc.getLong("qty") ?: 0L
                    val avg = doc.getDouble("avgPrice") ?: 0.0
                    Holding(stockId = sid, qty = qty, avgPrice = avg)
                } ?: emptyList()

            }
    }


    fun loadLeaderboard() {
        db.collection("leaderboard")
            .orderBy("totalCoins", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snap, _ ->
                leaderboard = snap?.documents?.map { doc ->
                    LeaderboardUser(
                        uid = doc.getString("uid") ?: "",
                        username = doc.getString("username") ?: "",
                        totalCoins = doc.getDouble("totalCoins")?.toInt() ?: 0
                    )
                } ?: emptyList()
            }
    }

    fun addCoins(amount: Long) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("coins", FieldValue.increment(amount))
            .addOnFailureListener { e -> Log.e("UserViewModel", "addCoins failed: ${e.message}") }
    }

    fun deductCoins(amount: Long) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("coins", FieldValue.increment(-amount))
            .addOnFailureListener { e -> Log.e("UserViewModel", "deductCoins failed: ${e.message}") }
    }

    fun updateCoins(amount: Long) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("coins", FieldValue.increment(amount))
            .addOnFailureListener { e -> Log.e("UserViewModel", "updateCoins failed: ${e.message}") }
    }



    private fun saveCoinsToDB(newCoins: Long) {
        val userId = auth.currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("coins", newCoins)
            .addOnSuccessListener { Log.d("UserViewModel", "Coins updated to $newCoins") }
            .addOnFailureListener { e -> Log.e("UserViewModel", "Failed to update coins", e) }
    }

    fun claimDailyCoins(amount: Long = 2675) {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(uid)

        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val lastTs = snap.getLong("lastRewardTimestamp") ?: 0L
            val now = System.currentTimeMillis()
            if (now - lastTs < 24L * 3600_000L) {
                // already claimed
                return@runTransaction null
            }
            val coins = (snap.getDouble("coins") ?: snap.getLong("coins")?.toDouble() ?: 0.0).toLong()
            val lifetime = (snap.getDouble("lifetimeCoins") ?: snap.getLong("lifetimeCoins")?.toDouble() ?: 0.0).toLong()

            tx.update(ref, mapOf(
                "coins" to coins + amount,
                "lifetimeCoins" to lifetime + amount,
                "lastRewardTimestamp" to now
            ))
            null
        }.addOnFailureListener { e ->
            android.util.Log.e("UserViewModel", "claimDailyCoins failed: ${e.message}")
        }
    }


    fun updateUsername(newUsername: String) {
        val ref = userRef ?: return
        ref.update("username", newUsername)
        username = newUsername
    }

    private fun crashDoc() = auth.currentUser?.uid?.let {
        db.collection("users").document(it).collection("gameState").document("crash")
    }

    fun observeCrashState(onChange: (playsToday: Int, lastResetAt: Long) -> Unit) {
        val ref = crashDoc() ?: return
        ref.addSnapshotListener { snap, _ ->
            val p = snap?.getLong("playsToday") ?: 0L
            val ts = snap?.getLong("lastResetAt") ?: 0L
            onChange(p.toInt(), ts)
        }
    }
    private suspend fun applyCrashOutcomeAtomic(bet: Long, multiplier: Double): Long? {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return null
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(uid)

        return try {
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                val coins = (snap.getDouble("coins") ?: snap.getLong("coins")?.toDouble() ?: 0.0).toLong()
                val lifetime = (snap.getDouble("lifetimeCoins") ?: snap.getLong("lifetimeCoins")?.toDouble() ?: 0.0).toLong()

                if (bet <= 0L || coins < bet) throw IllegalStateException("insufficient")

                val win = kotlin.math.floor(bet * multiplier).toLong()
                val net = win - bet
                val newCoins = coins - bet + win
                if (newCoins < 0L) throw IllegalStateException("negative_final")

                val updates = mutableMapOf<String, Any>(
                    "coins" to newCoins
                )
                if (net > 0) {
                    updates["lifetimeCoins"] = lifetime + net
                } else if (!snap.contains("lifetimeCoins")) {
                    // Ensure field exists even if no win yet
                    updates["lifetimeCoins"] = lifetime
                }

                tx.update(ref, updates as Map<String, Any>)
                net
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("CrashTxn", "applyCrashOutcomeAtomic failed: ${e.message}")
            null
        }
    }




    suspend fun tryConsumeCrashPlay(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val ref = db.collection("users").document(uid).collection("gameState").document("crash")
        return try {
            db.runTransaction { tx ->
                val now = System.currentTimeMillis()
                val snap = tx.get(ref)
                var plays = (snap.getLong("playsToday") ?: 0L).toInt()
                var lastReset = snap.getLong("lastResetAt") ?: 0L

                // Reset if 24h passed
                if (now - lastReset >= 24L * 3600_000L) {
                    plays = 0
                    lastReset = now
                }
                if (plays >= 10) {
                    throw IllegalStateException("limit")
                }
                plays += 1

                val data = hashMapOf<String, Any>(
                    "playsToday" to plays,
                    "lastResetAt" to lastReset
                )
                if (snap.exists()) tx.update(ref, data) else tx.set(ref, data)
                null
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun incCoins(delta: Long): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("users").document(uid)
                .update("coins", FieldValue.increment(delta))
                .await()
            true
        } catch (e: Exception) {
            Log.e("UserViewModel", "incCoins failed: ${e.message}")
            false
        }
    }

}
