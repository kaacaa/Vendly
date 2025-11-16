package com.katarina.vendly.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationRepository(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient
) {

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) //precizna
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) //gruba
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(interval: Long = 5000L): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(Exception("Missing location permissions"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, //zelimo da bude sto tacnije
            interval
        )
            .setMinUpdateIntervalMillis(interval / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    trySend(location).isSuccess
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, context.mainLooper)

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }
}