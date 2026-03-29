package com.example.resqnet.ui.shelters

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.data.model.Shelter
import com.example.resqnet.data.model.ShelterPresentation
import com.example.resqnet.util.SampleShelters
import kotlinx.coroutines.launch

class SheltersViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    private val currentLocation = MutableLiveData<Location?>(null)
    private val sharedShelter = MutableLiveData<Shelter?>(null)
    private val _shareStatus = MutableLiveData<String>()
    val shareStatus: LiveData<String> = _shareStatus
    val liveLocation = app.liveLocationTracker.location.asLiveData()
    val shelters: LiveData<List<ShelterPresentation>> = currentLocation.map { location ->
        val items = buildList {
            addAll(SampleShelters.shelters)
            sharedShelter.value?.let(::add)
        }
        items.map { shelter ->
            val distance = location?.distanceTo(Location("shelter").apply {
                latitude = shelter.latitude
                longitude = shelter.longitude
            })?.div(1000.0)
            ShelterPresentation(shelter = shelter, distanceKm = distance)
        }.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
    }

    fun updateLocation(location: Location?) {
        currentLocation.value = location
    }

    fun currentLocation(): Location? = currentLocation.value

    fun refreshCurrentLocation() {
        app.liveLocationTracker.requestFreshLocation()
    }

    fun addCurrentLocationShelter() {
        val location = currentLocation.value
        if (location == null) {
            _shareStatus.value = "Current GPS location unavailable. Please wait for a location fix."
            return
        }
        sharedShelter.value = Shelter(
            name = "Current Location Shelter",
            latitude = location.latitude,
            longitude = location.longitude,
            notes = "Shelter Available Here"
        )
        currentLocation.value = location
        _shareStatus.value = "Current location shelter added to the map."
    }

    fun shareCurrentLocationShelter() {
        val location = currentLocation.value
        if (location == null) {
            _shareStatus.value = "Current GPS location unavailable. Unable to share shelter."
            return
        }
        addCurrentLocationShelter()
        viewModelScope.launch {
            val queued = app.meshRepository.queueOutgoingMessage(
                type = MessageType.RESOURCE,
                content = "Shelter available at my current location",
                latitude = location.latitude,
                longitude = location.longitude
            )
            if (queued != null) {
                app.meshCoordinator.flushQueue()
                _shareStatus.postValue("Shelter location shared with nearby users.")
            } else {
                _shareStatus.postValue("Unable to share shelter location right now.")
            }
        }
    }
}
