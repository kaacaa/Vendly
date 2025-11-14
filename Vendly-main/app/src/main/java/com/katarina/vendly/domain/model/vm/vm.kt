package com.katarina.vendly.domain.model.vm

import java.io.Serializable

data class VendingMachine(
    val id: String = "",
    val name: String = "",                        // optional display name
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: Serializable = "",                    // photo of the vending machine
    val productType: String = "",                 // e.g., "pića", "grickalice"
    val status: String = "puni",                 // user comments
    val addedByUserId: String = "",               // who registered it
    val updatedAt: Long = 0L,                     // last update timestamp
    val createdAt: Long = 0L                      // when it was added
)