package com.katarina.vendly.ui.filters

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katarina.vendly.data.vending.VendingRepository
import com.katarina.vendly.domain.model.vm.VendingMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FilterViewModel(
    private val repo: VendingRepository = VendingRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(FiltersUiState())
    val ui: StateFlow<FiltersUiState> = _ui

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadAll() {
        viewModelScope.launch {
            _ui.update { it.copy(isFiltering = true, error = null) }
            try {
                val list = repo.getMachines()
                _ui.update { st ->
                    val filteredNow = applyPredicate(
                        list,
                        st.productType,
                        st.status,
                        st.updatedAfter,
                        st.updatedBefore
                    )
                    st.copy(
                        all = list,
                        filtered = filteredNow,
                        isFiltering = false,
                        filterActive = hasActiveFilters(
                            st.productType,
                            st.status,
                            st.updatedAfter,
                            st.updatedBefore
                        )
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isFiltering = false, error = e.message) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refresh() = loadAll()

    @RequiresApi(Build.VERSION_CODES.O)
    fun setProductType(v: String) {
        _ui.update { it.copy(productType = v) }
        reFilter()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setStatus(v: String) {
        _ui.update { it.copy(status = v) }
        reFilter()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setUpdatedAfter(v: String) {
        _ui.update { it.copy(updatedAfter = v) }
        reFilter()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setUpdatedBefore(v: String) {
        _ui.update { it.copy(updatedBefore = v) }
        reFilter()
    }

    fun clear() {
        _ui.update { st ->
            st.copy(
                productType = "",
                status = "",
                updatedAfter = "",
                updatedBefore = "",
                filtered = st.all,
                filterActive = false
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun reFilter() {
        _ui.update { st ->
            val filteredNow = applyPredicate(
                st.all,
                st.productType,
                st.status,
                st.updatedAfter,
                st.updatedBefore
            )
            st.copy(
                filtered = filteredNow,
                filterActive = hasActiveFilters(
                    st.productType,
                    st.status,
                    st.updatedAfter,
                    st.updatedBefore
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyPredicate(
        source: List<VendingMachine>,
        productType: String,
        status: String,
        updatedAfter: String,
        updatedBefore: String
    ): List<VendingMachine> {
        val (afterMillis, beforeMillis) = parseDateBounds(updatedAfter, updatedBefore)

        return source.filter { m ->
            if (productType.isNotBlank() &&
                !m.productType.equals(productType, ignoreCase = true)
            ) return@filter false

            if (status.isNotBlank() &&
                !m.status.equals(status, ignoreCase = true)
            ) return@filter false

            val updatedAt = m.updatedAt ?: m.createdAt // fallback if needed

            if (afterMillis != null && (updatedAt == null || updatedAt < afterMillis))
                return@filter false

            if (beforeMillis != null && (updatedAt == null || updatedAt > beforeMillis))
                return@filter false

            true
        }
    }

    private fun hasActiveFilters(
        productType: String,
        status: String,
        updatedAfter: String,
        updatedBefore: String
    ): Boolean {
        return productType.isNotBlank() ||
                status.isNotBlank() ||
                updatedAfter.isNotBlank() ||
                updatedBefore.isNotBlank()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDateBounds(after: String, before: String): Pair<Long?, Long?> {
        val zone = ZoneId.systemDefault()
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE

        val afterMs = after
            .takeIf { it.isNotBlank() }
            ?.runCatching {
                LocalDate.parse(this, fmt)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            }
            ?.getOrNull()

        val beforeMs = before
            .takeIf { it.isNotBlank() }
            ?.runCatching {
                // end of given day (23:59:59.999)
                LocalDate.parse(this, fmt)
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli() - 1
            }
            ?.getOrNull()

        return afterMs to beforeMs
    }
}