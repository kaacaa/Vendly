package com.katarina.vendly.ui.filters

import com.katarina.vendly.domain.model.vm.VendingMachine

data class FiltersUiState(
    // filter values (flat)
    val productType: String = "",
    val status: String = "",          // "full", "empty", "out_of_order", "low"
    val updatedAfter: String = "",    // yyyy-MM-dd (inclusive, start of day)
    val updatedBefore: String = "",   // yyyy-MM-dd (inclusive, end of day)

    // data
    val all: List<VendingMachine> = emptyList(),
    val filtered: List<VendingMachine> = emptyList(),

    // ui
    val isFiltering: Boolean = false,
    val filterActive: Boolean = false,
    val error: String? = null
)