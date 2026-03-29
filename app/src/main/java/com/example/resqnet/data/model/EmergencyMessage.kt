package com.example.resqnet.data.model

data class EmergencyMessage(
    val id: String,
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
    val retryCount: Int,
    val isLocalAuthor: Boolean
)
