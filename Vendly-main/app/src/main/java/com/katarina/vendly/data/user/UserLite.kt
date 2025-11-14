package com.katarina.vendly.data.user

data class UserLite(
    val uid: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null
)