package com.example.resqnet.ui.resources

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.example.resqnet.ResQNetApp
import com.example.resqnet.util.SampleHelpCenters

class ResourcesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp

    val currentLocation = app.liveLocationTracker.location.asLiveData()

    val resourceSummaries = currentLocation.map { location ->
        SampleHelpCenters.centers
            .map { center -> center to center.distanceFrom(location) }
            .sortedBy { it.second }
            .map { (center, distanceMeters) ->
                val label = if (distanceMeters == null) {
                    "Distance unavailable"
                } else {
                    "${"%.1f".format(distanceMeters / 1000.0)} km away"
                }
                center to label
            }
    }

    private fun com.example.resqnet.data.model.HelpCenter.distanceFrom(location: Location?): Float? {
        if (location == null) return null
        return Location("help-center").apply {
            latitude = this@distanceFrom.latitude
            longitude = this@distanceFrom.longitude
        }.distanceTo(location)
    }
}
