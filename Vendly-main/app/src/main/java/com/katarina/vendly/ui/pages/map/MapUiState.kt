package com.katarina.vendly.ui.pages.map

import com.katarina.vendly.domain.model.vm.VendingMachine

data class MapUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val vendingMachines: List<VendingMachine> = emptyList(),
    val error: String? = null,
    val selectedVending: VendingMachine? = null,
    val isFiltering: Boolean = false,
    val filterActive: Boolean = false
)
