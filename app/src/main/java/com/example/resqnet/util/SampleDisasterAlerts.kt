package com.example.resqnet.util

import com.example.resqnet.data.model.AlertPresentation

object SampleDisasterAlerts {
    fun current(): List<AlertPresentation> {
        val now = System.currentTimeMillis()
        return listOf(
            AlertPresentation(
                id = "seed-flood",
                type = "Flood",
                severity = "Severe",
                location = "Riverbank Safe Point",
                summary = "Water level rising near evacuation corridor and boat rescue zone.",
                timestamp = now - 18 * 60_000
            ),
            AlertPresentation(
                id = "seed-fire",
                type = "Fire",
                severity = "Moderate",
                location = "Metro School Shelter",
                summary = "Generator smoke detected. Fire team dispatched for containment checks.",
                timestamp = now - 42 * 60_000
            ),
            AlertPresentation(
                id = "seed-cyclone",
                type = "Cyclone",
                severity = "Severe",
                location = "Central Relief Camp",
                summary = "High-wind band crossing supply tents. Reinforce shelter perimeter.",
                timestamp = now - 67 * 60_000
            ),
            AlertPresentation(
                id = "seed-earthquake",
                type = "Earthquake",
                severity = "Low",
                location = "North Community Hall",
                summary = "Aftershock inspection requested for damaged walls and access path.",
                timestamp = now - 95 * 60_000
            )
        )
    }
}
