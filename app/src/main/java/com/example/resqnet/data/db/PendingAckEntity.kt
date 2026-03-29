package com.example.resqnet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_ack_queue")
data class PendingAckEntity(
    @PrimaryKey val messageId: String,
    val peerAddress: String?,
    val lastAttemptAt: Long,
    val retryCount: Int,
    val expiresAt: Long
)
