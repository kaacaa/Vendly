package com.katarina.vendly.ui.pages.profile

import android.net.Uri
import com.katarina.vendly.domain.model.user.UserStats

data class ProfileUiState(
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String? = null,
    val pickedPhoto: Uri? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,

    val points: Long = 0,
    val stats: UserStats = UserStats()
)
