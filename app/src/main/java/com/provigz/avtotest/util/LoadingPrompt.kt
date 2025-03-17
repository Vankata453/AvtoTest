package com.provigz.avtotest.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ComposeRawLoadingPrompt(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(30.dp)
        )
        Spacer(
            modifier = Modifier.width(16.dp)
        )
        Text(
            text = "$text...",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ComposeLoadingPrompt(text: String = "Зареждане") {
    ComposeRawLoadingPrompt(
        text,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ComposeLoadingDialog(
    text: String = "Зареждане",
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            ComposeRawLoadingPrompt(text)
        },
        confirmButton = {}
    )
}
