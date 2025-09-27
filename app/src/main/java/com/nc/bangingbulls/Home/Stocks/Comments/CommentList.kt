package com.nc.bangingbulls.Home.Stocks.Comments

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable

@Composable
fun CommentsList(
    comments: List<Comment>,
    onLike: (Comment) -> Unit,
    onDislike: (Comment) -> Unit,
    onReply: (Comment) -> Unit
) {
    LazyColumn {
        items(comments) { comment ->
            CommentItem(comment, onLike, onDislike, onReply)
        }
    }
}
