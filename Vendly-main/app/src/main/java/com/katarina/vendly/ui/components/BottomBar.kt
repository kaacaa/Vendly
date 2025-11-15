package com.katarina.vendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(
    currentRoute: String?,
    onHome: () -> Unit,
    onMap: () -> Unit,
    onLeaderboard: () -> Unit,
    onProfile: () -> Unit,
    height: Dp = 60.dp
) {
    val colors = MaterialTheme.colorScheme
    val inactiveFg = colors.onSurfaceVariant

    val isHome = currentRoute?.startsWith("home") == true
    val isMap = currentRoute?.startsWith("map") == true
    val isLeaderboard = currentRoute?.startsWith("leaderboard") == true
    val isProfile = currentRoute?.startsWith("profile") == true

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HOME
            IconButton(onClick = onHome) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isHome) colors.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(22.dp),
                        tint = if (isHome) colors.onPrimaryContainer else inactiveFg
                    )
                }
            }

            // MAP
            IconButton(onClick = onMap) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isMap) colors.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = "Map",
                        modifier = Modifier.size(22.dp),
                        tint = if (isMap) colors.onPrimaryContainer else inactiveFg
                    )
                }
            }

            // LEADERBOARD
            IconButton(onClick = onLeaderboard) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isLeaderboard) colors.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Leaderboard,
                        contentDescription = "Leaderboard",
                        modifier = Modifier.size(22.dp),
                        tint = if (isLeaderboard) colors.onPrimaryContainer else inactiveFg
                    )
                }
            }

            // PROFILE
            IconButton(onClick = onProfile) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isProfile) colors.secondaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(22.dp),
                        tint = if (isProfile) colors.onSecondaryContainer else inactiveFg
                    )
                }
            }
        }
    }
}