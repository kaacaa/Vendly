package com.katarina.vendly.ui.pages.home

data class HomeUiState(
    val isLoading: Boolean = true,
    val items: List<VendingItem> = emptyList(),
    val error: String? = null,
    val isSignedIn: Boolean = false
)
