package com.katarina.vendly.ui.pages.vendingdetails

import com.katarina.vendly.domain.model.vm.VendingMachine

data class VendingDetailsUiState(
    val vendingId: String,
    val vending: VendingMachine? = null,
    val loading: Boolean = true,
    val error: String? = null
)