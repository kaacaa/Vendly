package com.katarina.vendly.ui.pages.login


    data class LoginUiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )
