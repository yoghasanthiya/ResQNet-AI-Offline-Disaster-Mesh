package com.example.resqnet.data.model

data class AlertPresentation(
    val id: String,
    val type: String,
    val severity: String,
    val location: String,
    val summary: String,
    val timestamp: Long
)
