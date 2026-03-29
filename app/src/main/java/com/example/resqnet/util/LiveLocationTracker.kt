package com.example.resqnet.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LiveLocationTracker(
    private val application: Application
) {
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(application)
    }
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location
    private var started = false
    private val locationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .setWaitForAccurateLocation(true)
            .build()
    }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            _location.value = result.lastLocation ?: _location.value
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (started || !hasLocationPermission()) return
        started = true
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) _location.value = location
        }
        fusedClient.requestLocationUpdates(locationRequest, callback, application.mainLooper)
    }

    @SuppressLint("MissingPermission")
    fun requestFreshLocation() {
        if (!hasLocationPermission()) return
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    _location.value = location
                }
            }
    }

    @SuppressLint("MissingPermission")
    suspend fun awaitFreshLocation(timeoutMillis: Long = 6_000L): Location? {
        if (!hasLocationPermission()) return null
        val cached = _location.value
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        val resolved = location ?: cached
                        if (resolved != null) {
                            _location.value = resolved
                        }
                        if (continuation.isActive) {
                            continuation.resume(resolved)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(cached)
                        }
                    }
            }
        } ?: cached
    }

    fun stop() {
        if (!started) return
        started = false
        fusedClient.removeLocationUpdates(callback)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
