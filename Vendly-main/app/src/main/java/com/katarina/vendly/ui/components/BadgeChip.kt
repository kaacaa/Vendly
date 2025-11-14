package com.katarina.vendly.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.katarina.vendly.domain.gamification.Badge

private val Bronze   = Color(0xFFCD7F32)
private val Silver   = Color(0xFFC0C0C0)
private val Gold     = Color(0xFFFFD700)
private val Platinum = Color(0xFFE5E4E2)
private val Diamond  = Color(0xFFB9F2FF)

@Composable
fun BadgeChip(
    points: Long,
    modifier: Modifier = Modifier,
    showPoints: Boolean = true
) {
    val badge = Badge.fromPoints(points)
    val (bg, emoji) = when (badge) {
        Badge.NEWBIE -> Pair(MaterialTheme.colorScheme.surfaceVariant, "🌱")
        Badge.BRONZE -> Pair(Bronze, "🥉")
        Badge.SILVER -> Pair(Silver, "🥈")
        Badge.GOLD -> Pair(Gold, "🥇")
        Badge.PLATINUM -> Pair(Platinum, "🏆")
        Badge.DIAMOND -> Pair(Diamond, "💎")
    }
    val labelColor = MaterialTheme.colorScheme.contentColorFor(bg)

    val text = if (showPoints) "$emoji ${badge.label} • $points" else "$emoji ${badge.label}"

    AssistChip(
        onClick = {},
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = bg,
            labelColor = labelColor
        ),
        modifier = modifier
    )
}