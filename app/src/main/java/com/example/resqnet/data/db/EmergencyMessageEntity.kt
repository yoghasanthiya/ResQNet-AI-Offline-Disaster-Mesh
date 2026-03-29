package com.example.resqnet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.resqnet.data.model.DeliveryStatus
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.data.model.UserRole

@Entity(tableName = "emergency_messages")
data class EmergencyMessageEntity(
    @PrimaryKey val id: String,
    val senderName: String,
    val senderPhone: String,
    val senderRole: UserRole,
    val senderDeviceId: String,
    val type: MessageType,
    val content: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val priorityScore: Int,
    val encryptedPayload: String,
    val deliveryStatus: DeliveryStatus,
    val hopCount: Int,
    val ttl: Int,
    val acknowledgedAt: Long?,
    val lastRelayAttemptAt: Long?,
    val retryCount: Int,
    val isLocalAuthor: Boolean
)
