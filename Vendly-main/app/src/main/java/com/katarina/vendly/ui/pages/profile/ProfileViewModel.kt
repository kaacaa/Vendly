package com.katarina.vendly.ui.pages.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katarina.vendly.data.user.UserRepository
import com.katarina.vendly.domain.usecase.UploadProfileImageUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val uploader: UploadProfileImageUseCase = UploadProfileImageUseCase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui

    fun start() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepo.observeUser(uid).collect { user ->
                if (user != null) {
                    _ui.update {
                        it.copy(
                            email = user.email.orEmpty(),
                            fullName = user.fullName.orEmpty(),
                            phoneNumber = user.phoneNumber.orEmpty(),
                            profileImageUrl = user.profileImageUrl,
                            points = user.points,
                            stats = user.stats,
                            error = null,
                            saved = false
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(v: String) = _ui.update { it.copy(fullName = v, saved = false) }
    fun onPhoneChanged(v: String) = _ui.update { it.copy(phoneNumber = v, saved = false) }
    fun onPhotoPicked(uri: android.net.Uri) = _ui.update { it.copy(pickedPhoto = uri, saved = false) }

    fun save(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        val s = _ui.value

        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null, saved = false) }
            try {
                val imageUrl: String? = if (s.pickedPhoto != null) {
                    val result = uploader(context, s.pickedPhoto!!)
                    result.getOrElse { t ->
                        _ui.update {
                            it.copy(
                                isSaving = false,
                                error = "Upload failed: ${t.message ?: "unknown error"}",
                                saved = false
                            )
                        }
                        return@launch
                    }
                } else s.profileImageUrl

                userRepo.updateProfile(
                    uid = uid,
                    fullName = s.fullName.trim(),
                    phoneNumber = s.phoneNumber.trim(),
                    profileImageUrl = imageUrl
                )

                _ui.update {
                    it.copy(
                        isSaving = false,
                        profileImageUrl = imageUrl,
                        pickedPhoto = null,
                        saved = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save profile",
                        saved = false
                    )
                }
            }
        }
    }
}