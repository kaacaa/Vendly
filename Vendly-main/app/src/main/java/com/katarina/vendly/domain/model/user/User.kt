package com.katarina.vendly.domain.model.user

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String? = null,

    val points: Long = 0,
    val stats: UserStats = UserStats()
)