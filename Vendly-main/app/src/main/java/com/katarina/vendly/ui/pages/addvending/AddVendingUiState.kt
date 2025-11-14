package com.katarina.vendly.ui.pages.addvending

import android.net.Uri
import com.katarina.vendly.domain.model.vm.VendingStatus

data class AddVendingUiState(
    val name: String = "",
    val productType: String = "",
    // Default to canonical English code
    val status: String = VendingStatus.FULL.code,
    val photo: Uri? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)