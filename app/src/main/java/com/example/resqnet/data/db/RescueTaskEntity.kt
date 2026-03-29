package com.example.resqnet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.data.model.TaskStatus
import com.example.resqnet.data.model.VolunteerSkill

@Entity(tableName = "rescue_tasks")
data class RescueTaskEntity(
    @PrimaryKey val id: String,
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
