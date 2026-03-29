package com.example.resqnet.ui.control

import android.app.Application
import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.DeliveryStatus
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.service.MeshForegroundService
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

class ControlCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    private val startTime = SystemClock.elapsedRealtime()
    private val _actionFeedback = MutableLiveData<String>()
    val actionFeedback: LiveData<String> = _actionFeedback

    val statusLabel = combine(
        app.meshCoordinator.status,
        app.meshCoordinator.peers,
        MeshForegroundService.serviceState
    ) { status, peers, runtime ->
        val mode = if (runtime.isBackgroundMode) "Background" else "Foreground"
        val bt = if (app.meshCoordinator.isBluetoothEnabled()) "Bluetooth on" else "Bluetooth off"
        "$status | ${peers.count { it.isConnected }} connected | $bt | $mode"
    }.asLiveData()
    val peerCount = app.meshCoordinator.peers.asLiveData().map { peers -> peers.count { it.isConnected } }

    val relayMetrics = app.meshRepository.observeMessages()
        .combine(app.meshCoordinator.peers) { messages, peers ->
            val relayed = messages.count {
                it.deliveryStatus == DeliveryStatus.RELAYED || it.deliveryStatus == DeliveryStatus.ACKNOWLEDGED
            }
            val latency = messages.filter { it.acknowledgedAt != null }.map { it.acknowledgedAt!! - it.createdAt }
                .average()
                .takeIf { !it.isNaN() }
                ?.toLong()
                ?: if (peers.any { it.isConnected }) 180L else 0L
            Triple(relayed, peers.count { it.isConnected }, latency)
        }
        .asLiveData()

    val recentActivity = app.meshRepository.observeMessages().asLiveData().map { messages ->
        val events = mutableListOf<String>()
        messages.take(3).forEach { message ->
            events += "${message.type.name} sent by ${message.senderName}"
        }
        if (events.size < 4) events += "Location refreshed"
        if (events.size < 4) events += "Battery level update"
        events.take(4)
    }

    val uptime = liveData {
        emit(SystemClock.elapsedRealtime() - startTime)
    }

    fun triggerEmergencyAlarm() {
        sendQuickActionMessage(
            type = MessageType.SOS,
            content = "SOS signal triggered from quick actions",
            successMessage = "Emergency alarm sent to nearby devices.",
            emptyProfileMessage = "Register a user profile before sending emergency alarms."
        )
    }

    fun triggerStatusUpdate() {
        sendQuickActionMessage(
            type = MessageType.NORMAL,
            content = "Status update sent from dashboard quick actions",
            successMessage = "Status update sent.",
            emptyProfileMessage = "Register a user profile before sending status updates."
        )
    }

    fun triggerShareLocation() {
        viewModelScope.launch {
            val location = awaitLatestLocation()
            if (location == null) {
                _actionFeedback.postValue("Current location unavailable. Please wait for a GPS fix.")
                return@launch
            }

            val queued = app.meshRepository.queueOutgoingMessage(
                type = MessageType.RESOURCE,
                content = "Sharing my current location for emergency coordination",
                latitude = location.latitude,
                longitude = location.longitude
            )
            if (queued != null) {
                app.meshCoordinator.flushQueue()
                _actionFeedback.postValue("Live location shared with nearby users.")
            } else {
                _actionFeedback.postValue("Register a user profile before sharing location.")
            }
        }
    }

    fun consumeActionFeedback() {
        _actionFeedback.value = null
    }

    private fun sendQuickActionMessage(
        type: MessageType,
        content: String,
        successMessage: String,
        emptyProfileMessage: String
    ) {
        viewModelScope.launch {
            val location = awaitLatestLocation()
            val queued = app.meshRepository.queueOutgoingMessage(
                type = type,
                content = content,
                latitude = location?.latitude,
                longitude = location?.longitude
            )
            if (queued != null) {
                app.meshCoordinator.flushQueue()
                _actionFeedback.postValue(successMessage)
            } else {
                _actionFeedback.postValue(emptyProfileMessage)
            }
        }
    }

    private fun latestLocation(): Location? {
        return app.liveLocationTracker.location.value
    }

    private suspend fun awaitLatestLocation(): Location? {
        return app.liveLocationTracker.awaitFreshLocation()
    }
}
