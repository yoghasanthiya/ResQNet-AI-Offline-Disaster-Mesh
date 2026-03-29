package com.example.resqnet.data.model

data class RescueTask(
    val id: String,
    val messageId: String,
    val requesterName: String,
    val requesterPhone: String,
    val requestType: MessageType,
    val locationLabel: String?,
    val latitude: Double?,
    val longitude: Double?,
    val priorityScore: Int,
    val requiredSkill: VolunteerSkill?,
    val assignedVolunteerId: String?,
    val assignedVolunteerName: String?,
    val status: TaskStatus,
    val updatedAt: Long
)
