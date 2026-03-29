package com.example.resqnet.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationProvider {
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): Location? {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return manager.getProviders(true)
            .asSequence()
            .mapNotNull { provider -> manager.getLastKnownLocation(provider) }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = manager.getProviders(true)
        if (providers.isEmpty()) return getLastKnownLocation(context)

        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(
                    providers.first(),
                    signal,
                    context.mainExecutor
                ) { location ->
                    continuation.resume(location ?: getLastKnownLocation(context))
                }
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onProviderDisabled(provider: String) = Unit

                @Deprecated("Deprecated in Java")
                override fun onProviderEnabled(provider: String) = Unit
            }

            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            runCatching {
                manager.requestLocationUpdates(
                    providers.first(),
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }.onFailure {
                manager.removeUpdates(listener)
                if (continuation.isActive) {
                    continuation.resume(getLastKnownLocation(context))
                }
            }
        }
    }
}
