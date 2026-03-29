package com.example.resqnet.data.model

enum class DeliveryStatus {
    STORED,
    QUEUED,
    SENDING,
    FORWARDED,
    ACKNOWLEDGED,
    RETRY_PENDING,
    RECEIVED,
    RELAYED
}
