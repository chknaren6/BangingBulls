package com.nc.bangingbulls.Home.Stocks.Comments.V

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nc.bangingbulls.Home.Stocks.Comments.M.Comment

@Composable
fun CommentItem(
    comment: Comment,
    onLike: (Comment) -> Unit,
    onDislike: (Comment) -> Unit,
    onReply: (Comment) -> Unit,
    depth: Int = 0
) {
    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(comment.username)
                Text(comment.text)
            }
            Row {
                Text("Like", modifier = Modifier.clickable { onLike(comment) })
                Spacer(Modifier.width(4.dp))
                Text("Dislike", modifier = Modifier.clickable { onDislike(comment) })
                Spacer(Modifier.width(4.dp))
                Button(onClick = { onReply(comment) }) { Text("Reply") }
            }
        }

        comment.replies.forEach { reply ->
            CommentItem(
                comment = reply,
                onLike = onLike,
                onDislike = onDislike,
                onReply = onReply,
                depth = depth + 1
            )
        }
    }
}
