package com.katarina.vendly.ui.pages.home

data class VendingItem(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val productType: String = "",
    val status: String = "puni",
    val createdAt: Long = 0L
)
