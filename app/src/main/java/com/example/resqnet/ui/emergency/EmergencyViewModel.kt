package com.example.resqnet.ui.emergency

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.MessageType
import android.util.Log
import kotlinx.coroutines.launch

class EmergencyViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        private const val TAG = "EmergencyViewModel"
    }

    private val app = application as ResQNetApp
    private val _requestSubmissionStatus = MutableLiveData<String>()
    val requestSubmissionStatus: LiveData<String> = _requestSubmissionStatus
    val liveLocation = app.liveLocationTracker.location.asLiveData()

    fun sendEmergency(type: MessageType, content: String) {
        viewModelScope.launch {
            val location = app.liveLocationTracker.awaitFreshLocation()
            app.meshRepository.queueOutgoingMessage(type, content, location?.latitude, location?.longitude)
            app.meshCoordinator.flushQueue()
        }
    }

    fun submitRescueRequest(
        requesterName: String,
        locationLabel: String,
        peopleCount: String,
        emergencyType: String,
        description: String
    ) {
        Log.d(TAG, "submitRescueRequest called for $requesterName at $locationLabel")
        val mappedType = when (emergencyType) {
            "Medical" -> MessageType.MEDICAL
            "Flood", "Collapsed Structure", "Fire", "Evacuation" -> MessageType.SOS
            else -> MessageType.RESOURCE
        }
        val content = buildString {
            append("RESCUE REQUEST\n")
            append("Name: ").append(requesterName).append('\n')
            append("Location: ").append(locationLabel).append('\n')
            append("People: ").append(peopleCount).append('\n')
            append("Type: ").append(emergencyType).append('\n')
            append("Details: ").append(description)
        }
        viewModelScope.launch {
            val location = app.liveLocationTracker.awaitFreshLocation()
            val queued = app.meshRepository.queueOutgoingMessage(
                type = mappedType,
                content = content,
                latitude = location?.latitude,
                longitude = location?.longitude
            )
            if (queued != null) {
                Log.d(TAG, "Rescue request queued successfully with id=${queued.id}")
                app.meshCoordinator.flushQueue()
                _requestSubmissionStatus.postValue("Request submitted successfully")
            } else {
                Log.w(TAG, "Rescue request submission failed because no user profile is registered")
                _requestSubmissionStatus.postValue("Unable to submit request right now")
            }
        }
    }

    fun consumeRequestSubmissionStatus() {
        _requestSubmissionStatus.value = null
    }
}
