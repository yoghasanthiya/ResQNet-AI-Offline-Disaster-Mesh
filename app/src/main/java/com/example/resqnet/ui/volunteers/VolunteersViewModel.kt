package com.example.resqnet.ui.volunteers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.example.resqnet.ResQNetApp

class VolunteersViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    val peers = app.meshCoordinator.peers.asLiveData()
    val meshStatus = app.meshCoordinator.status.asLiveData()

    fun beginDiscovery() {
        app.meshCoordinator.start()
        app.meshCoordinator.scanNow()
    }

    fun connect(address: String) {
        app.meshCoordinator.connectToPeer(address)
    }

    fun isBluetoothSupported(): Boolean = app.meshCoordinator.isBluetoothSupported()

    fun isBluetoothEnabled(): Boolean = app.meshCoordinator.isBluetoothEnabled()

    fun reportStatus(message: String) {
        app.meshCoordinator.updateStatus(message)
    }
}
