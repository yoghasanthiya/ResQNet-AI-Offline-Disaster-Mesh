package com.example.resqnet.data.repository

import android.app.Application
import com.example.resqnet.data.db.EmergencyMessageEntity
import com.example.resqnet.data.db.KnownDeviceEntity
import com.example.resqnet.data.db.PendingAckEntity
import com.example.resqnet.data.db.RescueTaskEntity
import com.example.resqnet.data.db.ResQNetDao
import com.example.resqnet.data.model.DeliveryStatus
import com.example.resqnet.data.model.EmergencyMessage
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.data.model.RescueTask
import com.example.resqnet.data.model.TaskStatus
import com.example.resqnet.data.model.VolunteerSkill
import com.example.resqnet.mesh.MeshPacket
import com.example.resqnet.mesh.PacketKind
import com.example.resqnet.util.DeviceIdentity
import com.example.resqnet.util.MeshEncryption
import com.example.resqnet.util.PriorityScorer
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MeshRepository(
    private val dao: ResQNetDao,
    private val application: Application,
    private val userRepository: UserRepository
) {
    private val seenRelayIds = linkedSetOf<String>()

    fun observeMessages(): Flow<List<EmergencyMessage>> {
        val localDeviceId = DeviceIdentity.resolve(application)
        return dao.observeMessages().map { entities ->
            entities.map {
                EmergencyMessage(
                    id = it.id,
                    senderName = it.senderName,
                    senderPhone = it.senderPhone,
                    senderRole = it.senderRole,
                    senderDeviceId = it.senderDeviceId,
                    type = it.type,
                    content = it.content,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    createdAt = it.createdAt,
                    priorityScore = it.priorityScore,
                    encryptedPayload = it.encryptedPayload,
                    deliveryStatus = it.deliveryStatus,
                    hopCount = it.hopCount,
                    ttl = it.ttl,
                    acknowledgedAt = it.acknowledgedAt,
                    retryCount = it.retryCount,
                    isLocalAuthor = it.senderDeviceId == localDeviceId
                )
            }
        }
    }

    fun observeEmergencyRequests(): Flow<List<EmergencyMessage>> {
        val localDeviceId = DeviceIdentity.resolve(application)
        return dao.observeEmergencyMessages().map { entities ->
            entities.map {
                EmergencyMessage(
                    id = it.id,
                    senderName = it.senderName,
                    senderPhone = it.senderPhone,
                    senderRole = it.senderRole,
                    senderDeviceId = it.senderDeviceId,
                    type = it.type,
                    content = it.content,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    createdAt = it.createdAt,
                    priorityScore = it.priorityScore,
                    encryptedPayload = it.encryptedPayload,
                    deliveryStatus = it.deliveryStatus,
                    hopCount = it.hopCount,
                    ttl = it.ttl,
                    acknowledgedAt = it.acknowledgedAt,
                    retryCount = it.retryCount,
                    isLocalAuthor = it.senderDeviceId == localDeviceId
                )
            }
        }
    }

    fun observeKnownDevices(): Flow<List<KnownDeviceEntity>> = dao.observeKnownDevices()

    suspend fun queueOutgoingMessage(
        type: MessageType,
        content: String,
        latitude: Double?,
        longitude: Double?
    ): EmergencyMessageEntity? {
        val profile = userRepository.getUserProfile() ?: return null
        val deviceId = DeviceIdentity.resolve(application)
        val trimmedContent = content.trim()
        val encryptedPayload = MeshEncryption.encrypt(trimmedContent)
        val entity = EmergencyMessageEntity(
            id = UUID.randomUUID().toString(),
            senderName = profile.name,
            senderPhone = profile.phoneNumber,
            senderRole = profile.role,
            senderDeviceId = deviceId,
            type = type,
            content = trimmedContent,
            latitude = latitude,
            longitude = longitude,
            createdAt = System.currentTimeMillis(),
            priorityScore = PriorityScorer.score(type, trimmedContent),
            encryptedPayload = encryptedPayload,
            deliveryStatus = DeliveryStatus.QUEUED,
            hopCount = 0,
            ttl = DEFAULT_TTL,
            acknowledgedAt = null,
            lastRelayAttemptAt = null,
            retryCount = 0,
            isLocalAuthor = true
        )
        dao.insertMessage(entity)
        createTaskIfRequired(entity, profile.locationLabel)
        return entity
    }

    suspend fun pendingMeshPackets(): List<MeshPacket> {
        dao.clearExpiredPendingAcks(System.currentTimeMillis())
        return dao.getPendingRelayMessages()
            .filter { canForward(it) }
            .map(::toMessagePacket)
    }

    suspend fun getRetryPackets(): List<MeshPacket> {
        val now = System.currentTimeMillis()
        val retryEntries = dao.getPendingAckQueue(now)
        if (retryEntries.isEmpty()) return emptyList()
        val messages = dao.getMessagesByIds(retryEntries.map { it.messageId }).associateBy { it.id }
        return retryEntries.mapNotNull { entry ->
            val message = messages[entry.messageId] ?: return@mapNotNull null
            if (!canForward(message)) return@mapNotNull null
            toMessagePacket(message)
        }
    }

    suspend fun markSending(messageId: String, peerAddress: String?) {
        val existing = dao.getMessageById(messageId) ?: return
        val now = System.currentTimeMillis()
        dao.updateMessage(
            existing.copy(
                deliveryStatus = DeliveryStatus.SENDING,
                lastRelayAttemptAt = now
            )
        )
        dao.upsertPendingAck(
            PendingAckEntity(
                messageId = messageId,
                peerAddress = peerAddress,
                lastAttemptAt = now,
                retryCount = existing.retryCount,
                expiresAt = now + ACK_EXPIRY_MS
            )
        )
    }

    suspend fun markAcked(messageId: String) {
        val existing = dao.getMessageById(messageId) ?: return
        dao.updateMessage(
            existing.copy(
                deliveryStatus = DeliveryStatus.ACKNOWLEDGED,
                acknowledgedAt = System.currentTimeMillis()
            )
        )
        dao.deletePendingAck(messageId)
    }

    suspend fun markForwarded(messageId: String, hopCount: Int) {
        val existing = dao.getMessageById(messageId) ?: return
        val nextStatus = if (existing.isLocalAuthor) DeliveryStatus.SENDING else DeliveryStatus.RELAYED
        dao.updateMessage(
            existing.copy(
                deliveryStatus = nextStatus,
                hopCount = hopCount,
                lastRelayAttemptAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun registerRetryForPendingAcks() {
        val now = System.currentTimeMillis()
        val pending = dao.getPendingAckQueue(now)
        val messages = dao.getMessagesByIds(pending.map { it.messageId }).associateBy { it.id }
        pending.forEach { entry ->
            val message = messages[entry.messageId] ?: return@forEach
            dao.updateMessage(
                message.copy(
                    deliveryStatus = DeliveryStatus.RETRY_PENDING,
                    retryCount = message.retryCount + 1
                )
            )
            dao.upsertPendingAck(
                entry.copy(
                    lastAttemptAt = now,
                    retryCount = entry.retryCount + 1,
                    expiresAt = now + ACK_EXPIRY_MS
                )
            )
        }
    }

    suspend fun ingestIncomingPacket(packet: MeshPacket): IncomingPacketResult {
        return when (packet.packetKind) {
            PacketKind.ACK -> {
                packet.ackForMessageId?.let { markAcked(it) }
                IncomingPacketResult(processed = false, ackPacket = null)
            }

            PacketKind.MESSAGE -> {
                if (packet.hopCount > packet.ttl) {
                    return IncomingPacketResult(processed = false, ackPacket = ackFor(packet))
                }
                if (isDuplicateRelay(packet.id, packet.hopCount)) {
                    return IncomingPacketResult(processed = false, ackPacket = ackFor(packet))
                }

                val existing = dao.getMessageById(packet.id)
                if (existing != null && existing.hopCount >= packet.hopCount) {
                    return IncomingPacketResult(processed = false, ackPacket = ackFor(packet))
                }

                val content = MeshEncryption.decrypt(packet.encryptedPayload)
                val entity = EmergencyMessageEntity(
                    id = packet.id,
                    senderName = packet.senderName,
                    senderPhone = packet.senderPhone,
                    senderRole = packet.senderRole,
                    senderDeviceId = packet.senderDeviceId,
                    type = packet.type,
                    content = content,
                    latitude = packet.latitude,
                    longitude = packet.longitude,
                    createdAt = packet.createdAt,
                    priorityScore = packet.priorityScore,
                    encryptedPayload = packet.encryptedPayload,
                    deliveryStatus = DeliveryStatus.RECEIVED,
                    hopCount = packet.hopCount,
                    ttl = packet.ttl,
                    acknowledgedAt = null,
                    lastRelayAttemptAt = null,
                    retryCount = 0,
                    isLocalAuthor = false
                )
                if (existing == null) {
                    dao.insertMessage(entity)
                } else {
                    dao.updateMessage(entity)
                }
                createTaskIfRequired(entity, null)
                IncomingPacketResult(processed = true, ackPacket = ackFor(packet))
            }
        }
    }

    suspend fun rememberKnownDevice(address: String, name: String, connected: Boolean) {
        val existing = dao.getKnownDevice(address)
        val now = System.currentTimeMillis()
        dao.upsertKnownDevice(
            KnownDeviceEntity(
                address = address,
                name = name,
                lastSeenAt = now,
                lastConnectedAt = if (connected) now else existing?.lastConnectedAt,
                isPinned = existing?.isPinned ?: false
            )
        )
    }

    fun observeActiveTasks(): Flow<List<RescueTask>> =
        dao.observeActiveTasks().map { list -> list.map(::mapTask) }

    fun observeVolunteerTasks(volunteerId: String, volunteerName: String): Flow<List<RescueTask>> =
        dao.observeTasksForVolunteer(volunteerId, volunteerName).map { list -> list.map(::mapTask) }

    fun observeAllTasks(): Flow<List<RescueTask>> =
        dao.observeAllTasks().map { list -> list.map(::mapTask) }

    suspend fun acceptTask(taskId: String) {
        val profile = userRepository.getUserProfile() ?: return
        val task = dao.getTaskById(taskId) ?: return
        val volunteerId = DeviceIdentity.resolve(application)
        dao.upsertTask(
            task.copy(
                assignedVolunteerId = volunteerId,
                assignedVolunteerName = profile.name,
                status = TaskStatus.ACCEPTED,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        val task = dao.getTaskById(taskId) ?: return
        dao.upsertTask(task.copy(status = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun assignTask(taskId: String, volunteerName: String) {
        val task = dao.getTaskById(taskId) ?: return
        dao.upsertTask(
            task.copy(
                assignedVolunteerName = volunteerName.trim(),
                status = TaskStatus.ACCEPTED,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun createTaskIfRequired(message: EmergencyMessageEntity, locationLabel: String?) {
        if (message.type == MessageType.NORMAL) return
        val existing = dao.getTaskByMessageId(message.id)
        if (existing != null) return

        dao.upsertTask(
            RescueTaskEntity(
                id = UUID.randomUUID().toString(),
                messageId = message.id,
                requesterName = message.senderName,
                requesterPhone = message.senderPhone,
                requestType = message.type,
                locationLabel = locationLabel,
                latitude = message.latitude,
                longitude = message.longitude,
                priorityScore = message.priorityScore,
                requiredSkill = inferRequiredSkill(message.type, message.content),
                assignedVolunteerId = null,
                assignedVolunteerName = null,
                status = TaskStatus.OPEN,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun inferRequiredSkill(type: MessageType, content: String): VolunteerSkill? {
        val normalized = content.lowercase()
        return when {
            type == MessageType.MEDICAL -> VolunteerSkill.MEDICAL
            normalized.contains("transport") || normalized.contains("vehicle") || normalized.contains("evacuate") -> VolunteerSkill.TRANSPORT
            type == MessageType.RESOURCE -> VolunteerSkill.FOOD_SUPPLY
            else -> null
        }
    }

    private fun mapTask(entity: RescueTaskEntity): RescueTask {
        return RescueTask(
            id = entity.id,
            messageId = entity.messageId,
            requesterName = entity.requesterName,
            requesterPhone = entity.requesterPhone,
            requestType = entity.requestType,
            locationLabel = entity.locationLabel,
            latitude = entity.latitude,
            longitude = entity.longitude,
            priorityScore = entity.priorityScore,
            requiredSkill = entity.requiredSkill,
            assignedVolunteerId = entity.assignedVolunteerId,
            assignedVolunteerName = entity.assignedVolunteerName,
            status = entity.status,
            updatedAt = entity.updatedAt
        )
    }

    private fun ackFor(packet: MeshPacket): MeshPacket {
        return MeshPacket(
            id = UUID.randomUUID().toString(),
            packetKind = PacketKind.ACK,
            senderName = packet.senderName,
            senderPhone = packet.senderPhone,
            senderRole = packet.senderRole,
            senderDeviceId = DeviceIdentity.resolve(application),
            type = packet.type,
            encryptedPayload = "",
            latitude = null,
            longitude = null,
            createdAt = System.currentTimeMillis(),
            priorityScore = packet.priorityScore,
            hopCount = 0,
            ttl = 1,
            ackForMessageId = packet.id
        )
    }

    private fun toMessagePacket(entity: EmergencyMessageEntity): MeshPacket {
        return MeshPacket(
            id = entity.id,
            packetKind = PacketKind.MESSAGE,
            senderName = entity.senderName,
            senderPhone = entity.senderPhone,
            senderRole = entity.senderRole,
            senderDeviceId = entity.senderDeviceId,
            type = entity.type,
            encryptedPayload = entity.encryptedPayload,
            latitude = entity.latitude,
            longitude = entity.longitude,
            createdAt = entity.createdAt,
            priorityScore = entity.priorityScore,
            hopCount = entity.hopCount,
            ttl = entity.ttl
        )
    }

    private fun canForward(entity: EmergencyMessageEntity): Boolean {
        if (entity.hopCount >= entity.ttl) return false
        if (entity.deliveryStatus == DeliveryStatus.ACKNOWLEDGED) return false

        return if (entity.isLocalAuthor) {
            true
        } else {
            entity.deliveryStatus == DeliveryStatus.RECEIVED || entity.deliveryStatus == DeliveryStatus.RETRY_PENDING
        }
    }

    private fun isDuplicateRelay(messageId: String, hopCount: Int): Boolean {
        val key = "$messageId:$hopCount"
        val isDuplicate = seenRelayIds.contains(key)
        if (!isDuplicate) {
            seenRelayIds += key
            while (seenRelayIds.size > MAX_SEEN_CACHE_SIZE) {
                seenRelayIds.remove(seenRelayIds.first())
            }
        }
        return isDuplicate
    }

    data class IncomingPacketResult(
        val processed: Boolean,
        val ackPacket: MeshPacket?
    )

    companion object {
        private const val DEFAULT_TTL = 5
        private const val ACK_EXPIRY_MS = 45_000L
        private const val MAX_SEEN_CACHE_SIZE = 500
    }
}
