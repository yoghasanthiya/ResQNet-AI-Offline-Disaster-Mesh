package com.example.resqnet.ui.operations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.example.resqnet.ResQNetApp
import com.example.resqnet.util.SampleHelpCenters

class OperationsMapViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    val alerts = app.meshRepository.observeEmergencyRequests().asLiveData()
    val currentLocation = app.liveLocationTracker.location.asLiveData()
    val helpCenters = androidx.lifecycle.MutableLiveData(SampleHelpCenters.centers)

    fun refreshCurrentLocation() {
        app.liveLocationTracker.requestFreshLocation()
    }
}
