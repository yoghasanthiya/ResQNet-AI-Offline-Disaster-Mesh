package com.example.resqnet.ui.messages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.MessageType
import kotlinx.coroutines.launch

class MessagesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    val messages = app.meshRepository.observeMessages().asLiveData().map { messages ->
        messages.sortedWith(compareByDescending { it.createdAt })
    }
    val meshStatus = app.meshCoordinator.status.asLiveData()
    val liveLocation = app.liveLocationTracker.location.asLiveData()

    fun sendMessage(type: MessageType, content: String) {
        viewModelScope.launch {
            val location = app.liveLocationTracker.awaitFreshLocation()
            val queued = app.meshRepository.queueOutgoingMessage(
                type = type,
                content = content,
                latitude = location?.latitude,
                longitude = location?.longitude
            )
            if (queued != null) {
                app.meshCoordinator.flushQueue()
            }
        }
    }
}
