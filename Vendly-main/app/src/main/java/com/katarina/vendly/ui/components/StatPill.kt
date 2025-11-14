package com.katarina.vendly.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        label = { Text("$label: $value") },
        modifier = modifier
    )
}