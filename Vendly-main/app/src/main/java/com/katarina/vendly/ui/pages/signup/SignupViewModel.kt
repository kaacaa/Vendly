package com.katarina.vendly.ui.pages.signup

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katarina.vendly.data.user.UserRepository
import com.katarina.vendly.domain.usecase.UploadProfileImageUseCase
import com.katarina.vendly.ui.auth.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class SignupEvent {
    data object NavigateHome : SignupEvent()
    data class ShowToast(val message: String) : SignupEvent()
}

class SignupViewModel(
    private val uploadProfileImageUseCase: UploadProfileImageUseCase = UploadProfileImageUseCase(),
    private val userRepo: UserRepository = UserRepository(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    var uiState by mutableStateOf(SignupUiState())
        private set

    private var postAuthHandled = false

    private val _events = Channel<SignupEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(v: String) { uiState = uiState.copy(email = v) }
    fun onFullNameChanged(v: String) { uiState = uiState.copy(fullName = v) }
    fun onPhoneChanged(v: String) { uiState = uiState.copy(phoneNumber = v) }
    fun onPasswordChanged(v: String) { uiState = uiState.copy(password = v) }
    fun onPhotoPicked(uri: Uri?) { uiState = uiState.copy(photoUri = uri) }

    fun signup(authViewModel: AuthViewModel, context: Context) {
        val s = uiState
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            // postavi sliku na cloudinary ako postoji
            val profileUrl: String? = s.photoUri?.let { uri ->
                val result = uploadProfileImageUseCase(context, uri)
                result.getOrElse { t ->
                    _events.send(
                        SignupEvent.ShowToast(
                            "Photo upload failed: ${t.message ?: "unknown error"}"
                        )
                    )
                    null
                }
            }

            authViewModel.signup(
                s.email.trim(),
                s.password.trim(),
                s.fullName.trim(),
                s.phoneNumber.trim(),
                profileUrl
            )
        }
    }

    fun setError(message: String) {
        uiState = uiState.copy(isLoading = false, errorMessage = message)
    }

    fun setLoadingDone() {
        uiState = uiState.copy(isLoading = false)
    }

    fun handlePostAuthIfNeeded(context: Context) {
        if (postAuthHandled) return
        postAuthHandled = true

        viewModelScope.launch {
            try {
                val uid = firebaseAuth.currentUser?.uid
                if (uid == null) {
                    uiState = uiState.copy(isLoading = false)
                    _events.send(SignupEvent.ShowToast("No UID found after signup"))
                    return@launch
                }

                val url: String? = uiState.photoUri?.let { uri ->
                    val result = uploadProfileImageUseCase(context, uri)
                    result.getOrElse { t ->
                        _events.send(SignupEvent.ShowToast("Photo upload failed: ${t.message ?: "unknown error"}"))
                        null
                    }
                }

                runCatching {
                    userRepo.updateProfile(
                        uid = uid,
                        fullName = uiState.fullName.trim(),
                        phoneNumber = uiState.phoneNumber.trim(),
                        profileImageUrl = url
                    )
                }.onFailure {
                    _events.send(SignupEvent.ShowToast("Profile save failed: ${it.message}"))
                }

                uiState = uiState.copy(isLoading = false)
                _events.send(SignupEvent.NavigateHome)

            } catch (t: Throwable) {
                uiState = uiState.copy(isLoading = false)
                _events.send(SignupEvent.ShowToast("Unexpected error: ${t.message}"))
            }
        }
    }
}