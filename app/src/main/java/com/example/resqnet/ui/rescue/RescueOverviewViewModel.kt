package com.example.resqnet.ui.rescue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.AlertPresentation
import com.example.resqnet.data.model.MessageType

class RescueOverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    val alerts = app.meshRepository.observeMessages().asLiveData().map { messages ->
        messages
            .asSequence()
            .filter { !it.isLocalAuthor }
            .filter(::shouldCreateAlert)
            .map { message ->
            AlertPresentation(
                id = message.id,
                type = when (message.type) {
                    MessageType.SOS -> "SOS Alert"
                    MessageType.MEDICAL -> "Emergency Alert"
                    MessageType.RESOURCE -> {
                        if (message.content.contains("emergency", ignoreCase = true)) {
                            "Emergency Alert"
                        } else {
                            "Resource Alert"
                        }
                    }
                    MessageType.NORMAL -> "Emergency Alert"
                },
                severity = when (message.type) {
                    MessageType.SOS -> "Severe"
                    MessageType.MEDICAL -> "High"
                    MessageType.RESOURCE -> {
                        if (message.content.contains("emergency", ignoreCase = true)) "High" else "Moderate"
                    }
                    MessageType.NORMAL -> "High"
                },
                location = message.latitude?.let { lat ->
                    message.longitude?.let { lon ->
                        "Lat ${"%.3f".format(lat)}, Lon ${"%.3f".format(lon)}"
                    } ?: "Lat ${"%.3f".format(lat)}"
                } ?: "Location unavailable",
                summary = buildString {
                    append(message.senderName)
                    append(": ")
                    append(message.content)
                },
                timestamp = message.createdAt
            )
        }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<AlertPresentation> { severityRank(it.severity) }
                    .thenByDescending { it.timestamp }
            )
            .toList()
    }

    private fun shouldCreateAlert(message: com.example.resqnet.data.model.EmergencyMessage): Boolean {
        return message.type == MessageType.SOS ||
            message.type == MessageType.MEDICAL ||
            message.content.contains("emergency", ignoreCase = true)
    }

    private fun severityRank(severity: String): Int {
        return when (severity) {
            "Severe" -> 3
            "High" -> 2
            "Moderate" -> 1
            else -> 0
        }
    }
}
