package com.example.resqnet.data.model

data class DiscoveredPeer(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val isConnecting: Boolean,
    val rssi: Int?,
    val lastSeenAt: Long
)
