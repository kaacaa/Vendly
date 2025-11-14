package com.katarina.vendly.ui.pages.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class HomeViewModel : ViewModel() {
    private val _ui = MutableStateFlow(HomeUiState())
    val ui = _ui.asStateFlow()

    private var reg: ListenerRegistration? = null

    fun start() {
        listen()
    }

    fun restart() {
        reg?.remove()
        listen()
    }

    private fun listen() {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) {
            _ui.value = HomeUiState(isLoading = false, isSignedIn = false)
            return
        }

        _ui.value = _ui.value.copy(isLoading = true, error = null, isSignedIn = true)
        reg?.remove()

        reg = Firebase.firestore.collection("vending")
            .whereEqualTo("addedByUserId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    // Surface the exact message (e.g., "Missing or insufficient permissions.")
                    _ui.value = _ui.value.copy(isLoading = false, error = e.message)
                    return@addSnapshotListener
                }

                val docs = snap?.documents.orEmpty()
                val list = docs.map { d ->
                    VendingItem(
                        id = d.id,
                        name = d.getString("name") ?: "(unnamed)",
                        imageUrl = d.getString("imageUrl"),
                        productType = d.getString("productType") ?: "",
                        status = d.getString("status") ?: "puni",
                        createdAt = when (val t = d.get("createdAt")) {
                            is Timestamp -> t.toDate().time
                            is Number -> t.toLong()
                            else -> 0L
                        }
                    )
                }

                _ui.value = _ui.value.copy(
                    isLoading = false,
                    items = list,
                    error = null,
                    isSignedIn = true
                )
            }
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}