package com.katarina.vendly.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.katarina.vendly.domain.gamification.Badge

@Composable
fun NextBadgeProgress(points: Long, modifier: Modifier = Modifier) {
    val current = Badge.fromPoints(points)
    val nextTarget = Badge.nextTarget(points)

    if (nextTarget == null) {
        Text("Max badge reached: ${current.label}", style = MaterialTheme.typography.bodySmall)
        return
    }

    val start = current.minPoints
    val end = nextTarget
    val progress = ((points - start).toFloat() / (end - start).toFloat())
        .coerceIn(0f, 1f)

    Column(modifier) {
        LinearProgressIndicator(
            progress = { progress }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${end - points} pts to ${Badge.fromPoints(end).label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}