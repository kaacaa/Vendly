package com.katarina.vendly.ui.pages.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    navController: NavController,
    vm: LeaderboardViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(64.dp),
                title = {
                    Text(
                        "Leaderboard",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { inner ->
        when {
            ui.loading -> {
                Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            ui.error != null -> {
                Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                    Text(ui.error ?: "Error")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                ) {
                    // Top users
                    itemsIndexed(ui.users, key = { _, u -> u.uid }) { index, user ->
                        val rank = index + 1
                        val isMe = user.uid == ui.meUid
                        LeaderboardRow(
                            rank = rank,
                            name = user.displayName,
                            photoUrl = user.photoUrl,
                            points = user.points,
                            highlight = isMe
                        )
                        Divider()
                    }

                    // Show my rank if outside top N
                    ui.meOutsideTop?.let { me ->
                        ui.meRank?.let { rank ->
                            item { Spacer(Modifier.height(8.dp)) }
                            item {
                                Text(
                                    "Your position",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item {
                                LeaderboardRow(
                                    rank = rank,
                                    name = me.displayName,
                                    photoUrl = me.photoUrl,
                                    points = me.points,
                                    highlight = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    name: String,
    photoUrl: String?,
    points: Long,
    highlight: Boolean
) {
    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val rowBg = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small)
                .background(medalColor),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.width(12.dp))

        // Profile photo
        val ctx = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(photoUrl)
                .crossfade(true)
                .scale(Scale.FILL)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        Spacer(Modifier.width(12.dp))

        // User info
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = "$points pts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}