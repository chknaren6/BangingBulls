package com.nc.bangingbulls.Home.Stocks.Comments.M

data class Comment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val likes: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val replies: List<Comment> = emptyList(),
    val ts: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Comment {
            val likes = (map["likes"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val dislikes = (map["dislikes"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val replies = (map["replies"] as? List<*>)?.mapNotNull {
                if (it is Map<*, *>) {
                    val m = it as Map<String, Any?>
                    fromMap(m["id"]?.toString() ?: "", m)
                } else null
            } ?: emptyList()
            return Comment(
                id = id,
                userId = map["userId"]?.toString() ?: "",
                username = map["username"]?.toString() ?: "Anonymous",
                text = map["text"]?.toString() ?: "",
                likes = likes,
                dislikes = dislikes,
                replies = replies,
                ts = (map["ts"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}