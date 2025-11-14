package com.katarina.vendly.ui.pages.leaderboard

import com.katarina.vendly.data.user.LeaderboardUser

data class LeaderboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val users: List<LeaderboardUser> = emptyList(), // top users
    val meUid: String? = null,
    val meOutsideTop: LeaderboardUser? = null,
    val meRank: Int? = null
)