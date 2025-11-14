package com.katarina.vendly.ui.pages.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.katarina.vendly.ui.auth.AuthViewModel

class LoginViewModel : ViewModel() {
    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChanged(value: String) {
        uiState = uiState.copy(email = value)
    }

    fun onPasswordChanged(value: String) {
        uiState = uiState.copy(password = value)
    }

    fun login(authViewModel: AuthViewModel) {
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        authViewModel.login(uiState.email.trim(), uiState.password.trim())
    }

    fun setError(message: String) {
        uiState = uiState.copy(isLoading = false, errorMessage = message)
    }

    fun setLoadingDone() {
        uiState = uiState.copy(isLoading = false)
    }
}