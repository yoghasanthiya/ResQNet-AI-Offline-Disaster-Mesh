package com.example.resqnet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "known_devices")
data class KnownDeviceEntity(
    @PrimaryKey val address: String,
    val name: String,
    val lastSeenAt: Long,
    val lastConnectedAt: Long?,
    val isPinned: Boolean = false
)
