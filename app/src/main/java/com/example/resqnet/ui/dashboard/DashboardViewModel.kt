package com.example.resqnet.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.UserRole
import com.example.resqnet.service.MeshForegroundService
import kotlinx.coroutines.flow.combine

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp

    val userHeader = app.userRepository.observeUserProfile().asLiveData().map { profile ->
        if (profile == null) "" else "${profile.name} | ${profile.phoneNumber} | ${profile.locationLabel}"
    }

    val meshStatus = combine(
        app.meshCoordinator.status,
        app.meshCoordinator.peers,
        MeshForegroundService.serviceState
    ) { status, peers, runtime ->
        val bluetoothState = if (app.meshCoordinator.isBluetoothEnabled()) "Bluetooth on" else "Bluetooth off"
        val serviceMode = if (runtime.isRunning) {
            if (runtime.isBackgroundMode) "Background mode active" else "Foreground mode active"
        } else {
            "Mesh service stopped"
        }
        "$status • ${peers.count { it.isConnected }} peers • $bluetoothState • $serviceMode"
    }.asLiveData()
    val currentRole = app.userRepository.observeUserProfile().asLiveData().map { it?.role ?: UserRole.CIVILIAN }
}
