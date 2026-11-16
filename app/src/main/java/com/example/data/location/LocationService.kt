package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.example.domain.model.LocationFix
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationService(context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownFix(): LocationFix? {
        return try {
            val loc = fusedClient.lastLocation.await() ?: return null
            LocationFix(
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracyMeters = loc.accuracy,
                speedMps = loc.speed,
                bearing = loc.bearing,
                timestampMs = loc.time
            )
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLiveGps(intervalMs: Long = 2500L): Flow<LocationFix> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(2.0f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    LocationFix(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        speedMps = loc.speed,
                        bearing = loc.bearing,
                        timestampMs = loc.time
                    )
                )
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
