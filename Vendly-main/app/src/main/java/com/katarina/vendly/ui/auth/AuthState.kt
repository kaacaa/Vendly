package com.katarina.vendly.ui.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}