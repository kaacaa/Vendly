package com.katarina.vendly.domain.model.vm

import java.io.Serializable

data class VendingMachine(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: Serializable = "",
    val productType: String = "",
    val status: String = "puni",
    val addedByUserId: String = "",
    val updatedAt: Long = 0L,
    val createdAt: Long = 0L
)