package com.nc.bangingbulls.Home.Stocks.Comments

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CommentInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    replyingTo: Comment? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        TextField(
            value = if (replyingTo != null) "@${replyingTo.username} $text" else text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Write a comment...") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onSend) {
            Text(if (replyingTo != null) "Reply" else "Post")
        }
    }
}

