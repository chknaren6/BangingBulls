package com.nc.bangingbulls.Home.Stocks.Comments.V

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.nc.bangingbulls.Home.Stocks.Comments.M.Comment

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
