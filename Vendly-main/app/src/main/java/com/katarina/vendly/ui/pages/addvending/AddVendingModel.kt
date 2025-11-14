package com.katarina.vendly.ui.pages.addvending

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katarina.vendly.domain.model.vm.VendingStatus
import com.katarina.vendly.ui.pages.map.MapViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddVendingViewModel : ViewModel() {

    private val _ui = MutableStateFlow(AddVendingUiState())
    val ui: StateFlow<AddVendingUiState> = _ui

    fun onNameChanged(v: String)        = _ui.update { it.copy(name = v, error = null) }
    fun onProductTypeChanged(v: String) = _ui.update { it.copy(productType = v, error = null) }
    fun onStatusChanged(v: String)      = _ui.update { it.copy(status = v) } // expects canonical code
    fun onPhotoPicked(uri: android.net.Uri) = _ui.update { it.copy(photo = uri, error = null) }

    fun submit(context: Context, mapViewModel: MapViewModel) {
        val s = _ui.value
        if (s.photo == null || s.name.isBlank() || s.productType.isBlank()) {
            _ui.update { it.copy(error = "Please add a photo, name, and product type.") }
            return
        }

        val statusCode = VendingStatus.normalize(s.status)

        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null) }

            val res = mapViewModel.createVendingMachine(
                context = context,
                name = s.name.trim(),
                productType = s.productType.trim(),
                status = statusCode, // canonical code
                imageUri = s.photo!!
            )

            _ui.update {
                if (res.isSuccess) it.copy(isSaving = false, done = true)
                else it.copy(
                    isSaving = false,
                    error = res.exceptionOrNull()?.message ?: "Failed to add vending machine"
                )
            }
        }
    }

    fun resetDone() = _ui.update { it.copy(done = false) }
}