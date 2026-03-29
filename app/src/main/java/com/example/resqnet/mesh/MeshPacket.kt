package com.example.resqnet.mesh

import com.example.resqnet.data.model.MessageType
import com.example.resqnet.data.model.UserRole
import org.json.JSONObject

data class MeshPacket(
    val id: String,
    val packetKind: PacketKind,
    val senderName: String,
    val senderPhone: String,
    val senderRole: UserRole,
    val senderDeviceId: String,
    val type: MessageType,
    val encryptedPayload: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val priorityScore: Int,
    val hopCount: Int,
    val ttl: Int,
    val ackForMessageId: String? = null
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("packetKind", packetKind.name)
            put("senderName", senderName)
            put("senderPhone", senderPhone)
            put("senderRole", senderRole.name)
            put("senderDeviceId", senderDeviceId)
            put("type", type.name)
            put("encryptedPayload", encryptedPayload)
            put("createdAt", createdAt)
            put("priorityScore", priorityScore)
            put("hopCount", hopCount)
            put("ttl", ttl)
            if (ackForMessageId != null) put("ackForMessageId", ackForMessageId)
            if (latitude != null) put("latitude", latitude)
            if (longitude != null) put("longitude", longitude)
        }.toString()
    }

    companion object {
        fun fromJson(value: String): MeshPacket {
            val json = JSONObject(value)
            return MeshPacket(
                id = json.getString("id"),
                packetKind = json.optString("packetKind")
                    .takeIf { it.isNotBlank() }
                    ?.let { PacketKind.valueOf(it) }
                    ?: PacketKind.MESSAGE,
                senderName = json.getString("senderName"),
                senderPhone = json.getString("senderPhone"),
                senderRole = UserRole.valueOf(json.getString("senderRole")),
                senderDeviceId = json.getString("senderDeviceId"),
                type = MessageType.valueOf(json.getString("type")),
                encryptedPayload = json.getString("encryptedPayload"),
                latitude = json.optDouble("latitude").takeUnless { it.isNaN() },
                longitude = json.optDouble("longitude").takeUnless { it.isNaN() },
                createdAt = json.getLong("createdAt"),
                priorityScore = json.getInt("priorityScore"),
                hopCount = json.getInt("hopCount"),
                ttl = json.optInt("ttl", 5),
                ackForMessageId = json.optString("ackForMessageId").takeIf { it.isNotBlank() }
            )
        }
    }
}
