package com.katarina.vendly.ui.filters

import com.katarina.vendly.domain.model.vm.VendingMachine

data class FiltersUiState(
    val productType: String = "",
    val status: String = "",
    val updatedAfter: String = "",
    val updatedBefore: String = "",

    val all: List<VendingMachine> = emptyList(),
    val filtered: List<VendingMachine> = emptyList(),

    val isFiltering: Boolean = false,
    val filterActive: Boolean = false,
    val error: String? = null
)