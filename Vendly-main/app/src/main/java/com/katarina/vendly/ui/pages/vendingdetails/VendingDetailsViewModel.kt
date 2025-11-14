package com.katarina.vendly.ui.pages.vendingdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.katarina.vendly.data.user.UserRepository
import com.katarina.vendly.data.vending.VendingRepository
import com.katarina.vendly.domain.model.vm.VendingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VendingDetailsViewModel(
    private val vendingRepo: VendingRepository = VendingRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow<VendingDetailsUiState?>(null)
    val ui: StateFlow<VendingDetailsUiState?> = _ui

    fun start(vendingId: String) {
        val current = _ui.value
        if (current?.vendingId == vendingId && current.vending != null) return

        _ui.value = VendingDetailsUiState(vendingId = vendingId, loading = true)

        viewModelScope.launch {
            try {
                val vending = vendingRepo.getMachineById(vendingId)
                _ui.update { it?.copy(vending = vending, loading = false, error = null) }
            } catch (e: Exception) {
                _ui.update { it?.copy(loading = false, error = e.message ?: "Failed to load vending machine") }
            }
        }
    }

    fun updateStatus(newStatus: String) {
        val state = _ui.value ?: return
        val uid = auth.currentUser?.uid ?: return

        val normalized = VendingStatus.normalize(newStatus)

        viewModelScope.launch {
            try {
                vendingRepo.updateStatus(state.vendingId, normalized, actorUid = uid)
                userRepo.awardPointsForStatusUpdate(uid)
                _ui.update { it?.copy(vending = it.vending?.copy(status = normalized), error = null) }
            } catch (e: Exception) {
                _ui.update { it?.copy(error = e.message ?: "Failed to update status") }
            }
        }
    }
}