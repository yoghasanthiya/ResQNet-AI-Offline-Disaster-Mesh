package com.example.resqnet.ui.settings

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.example.resqnet.ResQNetApp

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    private val bootAt = SystemClock.elapsedRealtime()

    val profile = app.userRepository.observeUserProfile().asLiveData()
    val stats = app.meshRepository.observeMessages().asLiveData().map { messages ->
        val sent = messages.count { it.isLocalAuthor }
        val received = messages.count { !it.isLocalAuthor }
        val avg = messages.filter { it.acknowledgedAt != null }.map { it.acknowledgedAt!! - it.createdAt }
            .average()
            .takeIf { !it.isNaN() }
            ?.toLong()
            ?: 0L
        Triple(sent, received, avg)
    }
    val uptime = liveData { emit(SystemClock.elapsedRealtime() - bootAt) }
}
