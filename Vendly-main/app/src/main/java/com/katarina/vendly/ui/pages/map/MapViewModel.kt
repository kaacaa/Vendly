package com.katarina.vendly.ui.pages.map

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.katarina.vendly.data.location.LocationRepository
import com.katarina.vendly.data.user.UserRepository
import com.katarina.vendly.data.vending.VendingRepository
import com.katarina.vendly.domain.model.vm.VendingMachine
import com.katarina.vendly.domain.model.vm.VendingStatus
import com.katarina.vendly.domain.usecase.UploadProfileImageUseCase
import com.katarina.vendly.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_MOVE_METERS = 10f

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository: LocationRepository
    private val vendingRepository: VendingRepository = VendingRepository()
    private val userRepository: UserRepository = UserRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val uploader = UploadProfileImageUseCase()

    private var started = false
    private var updatesJob: Job? = null

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    private val notifier = NotificationHelper(getApplication<Application>().applicationContext)

    init {
        val fusedClient = LocationServices.getFusedLocationProviderClient(application)
        locationRepository = LocationRepository(application, fusedClient)
        fetchVendingMachines()
    }

    fun start() {
        if (started) return
        started = true

        updatesJob = viewModelScope.launch {
            locationRepository.getLocationUpdates()
                .distinctUntilChanged { old, new -> old.distanceTo(new) < MIN_MOVE_METERS }
                .catch { e -> _uiState.update { it.copy(error = e.message ?: "Location error") } }
                .collect { location ->
                    _uiState.update {
                        it.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            error = null
                        )
                    }

                    auth.currentUser?.uid?.let { uid ->
                        userRepository.updateUserLocation(uid, location.latitude, location.longitude)
                    }

                    checkVendingNearby(location.latitude, location.longitude)
                }
        }

        userRepository.observeUsers { users ->
            checkNearbyUsers(users)
        }
    }

    fun stop() {
        updatesJob?.cancel()
        updatesJob = null
        started = false
    }

    private fun checkVendingNearby(lat: Double, lng: Double) {
        val me = Location("").apply { latitude = lat; longitude = lng }

        uiState.value.vendingMachines.firstOrNull { v ->
            val target = Location("").apply { latitude = v.latitude; longitude = v.longitude }
            me.distanceTo(target) < 50f // 50m
        }?.let { v ->
            notifier.showVendingNotification(
                vendingId = v.id,
                title = "Nearby vending machine!",
                message = "You're close to ${v.name}"
            )
        }
    }

    private fun checkNearbyUsers(users: List<Triple<String, String, LatLng>>) {
        val myLat = uiState.value.latitude ?: return
        val myLng = uiState.value.longitude ?: return

        val me = Location("").apply { latitude = myLat; longitude = myLng }

        for ((uid, name, latLng) in users) {
            if (uid == auth.currentUser?.uid) continue

            val other = Location("").apply {
                latitude = latLng.latitude
                longitude = latLng.longitude
            }
            val distance = me.distanceTo(other)
            if (distance <= 50f) {
                notifier.showSimpleNotification(
                    title = "User nearby!",
                    message = "$name is within ${distance.toInt()} meters."
                )
            }
        }
    }

    private fun fetchVendingMachines() {
        viewModelScope.launch {
            try {
                val list = vendingRepository.getMachines()
                _uiState.update { it.copy(vendingMachines = list, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load machines") }
            }
        }
    }

    override fun onCleared() {
        updatesJob?.cancel()
        super.onCleared()
    }

    fun onVendingMarkerClicked(vending: VendingMachine?) {
        _uiState.update { it.copy(selectedVending = vending) }
    }

    fun dismissVendingDetails() {
        _uiState.update { it.copy(selectedVending = null) }
    }

    suspend fun createVendingMachine(
        context: Context,
        name: String,
        productType: String,
        status: String,
        imageUri: Uri
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not logged in"))

        return try {
            val imageUrl = uploader(context, imageUri).getOrElse { t ->
                return Result.failure(IllegalStateException("Upload failed: ${t.message ?: "unknown error"}"))
            }

            val lat = uiState.value.latitude ?: 0.0
            val lng = uiState.value.longitude ?: 0.0

            val machine = VendingMachine(
                id = "",
                name = name.trim(),
                productType = productType.trim(),
                status = VendingStatus.fromCode(status).code, // canonical
                latitude = lat,
                longitude = lng,
                imageUrl = imageUrl,
                addedByUserId = uid,
                createdAt = 0L,
                updatedAt = 0L
            )

            val id = vendingRepository.addMachine(machine)

            userRepository.awardPointsForNewVendingMachine(uid)

            fetchVendingMachines()

            Result.success(id)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun updateVendingStatus(
        vendingId: String,
        newStatus: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            vendingRepository.updateStatus(vendingId, newStatus, actorUid = uid)
            userRepository.awardPointsForStatusUpdate(uid)
            _uiState.update { state ->
                val updated = state.vendingMachines.map {
                    if (it.id == vendingId) it.copy(status = VendingStatus.normalize(newStatus)) else it
                }
                state.copy(vendingMachines = updated, selectedVending = state.selectedVending?.copy(status = VendingStatus.normalize(newStatus)))
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}