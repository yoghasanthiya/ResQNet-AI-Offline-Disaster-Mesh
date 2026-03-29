package com.example.resqnet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ResQNetDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM emergency_messages WHERE type != 'NORMAL' ORDER BY priorityScore DESC, createdAt DESC")
    fun observeEmergencyMessages(): Flow<List<EmergencyMessageEntity>>

    @Query("SELECT * FROM emergency_messages ORDER BY createdAt DESC, priorityScore DESC")
    fun observeMessages(): Flow<List<EmergencyMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: EmergencyMessageEntity): Long

    @Update
    suspend fun updateMessage(message: EmergencyMessageEntity)

    @Query("SELECT * FROM emergency_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): EmergencyMessageEntity?

    @Query("SELECT * FROM emergency_messages WHERE deliveryStatus IN ('STORED', 'QUEUED', 'RECEIVED', 'RELAYED', 'RETRY_PENDING', 'SENDING') ORDER BY priorityScore DESC, createdAt ASC")
    suspend fun getPendingRelayMessages(): List<EmergencyMessageEntity>

    @Query("SELECT * FROM emergency_messages WHERE id IN (:messageIds)")
    suspend fun getMessagesByIds(messageIds: List<String>): List<EmergencyMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKnownDevice(device: KnownDeviceEntity)

    @Query("SELECT * FROM known_devices ORDER BY COALESCE(lastConnectedAt, 0) DESC, lastSeenAt DESC")
    fun observeKnownDevices(): Flow<List<KnownDeviceEntity>>

    @Query("SELECT * FROM known_devices WHERE address = :address LIMIT 1")
    suspend fun getKnownDevice(address: String): KnownDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingAck(entry: PendingAckEntity)

    @Query("DELETE FROM pending_ack_queue WHERE messageId = :messageId")
    suspend fun deletePendingAck(messageId: String)

    @Query("SELECT * FROM pending_ack_queue WHERE expiresAt > :now ORDER BY lastAttemptAt ASC")
    suspend fun getPendingAckQueue(now: Long): List<PendingAckEntity>

    @Query("DELETE FROM pending_ack_queue WHERE expiresAt <= :now")
    suspend fun clearExpiredPendingAcks(now: Long)

    @Query("SELECT * FROM rescue_tasks ORDER BY priorityScore DESC, updatedAt DESC")
    fun observeAllTasks(): Flow<List<RescueTaskEntity>>

    @Query("SELECT * FROM rescue_tasks WHERE status IN ('OPEN', 'ACCEPTED', 'IN_PROGRESS') ORDER BY priorityScore DESC, updatedAt DESC")
    fun observeActiveTasks(): Flow<List<RescueTaskEntity>>

    @Query("SELECT * FROM rescue_tasks WHERE assignedVolunteerId = :volunteerId OR assignedVolunteerName = :volunteerName ORDER BY updatedAt DESC")
    fun observeTasksForVolunteer(volunteerId: String, volunteerName: String): Flow<List<RescueTaskEntity>>

    @Query("SELECT * FROM rescue_tasks WHERE messageId = :messageId LIMIT 1")
    suspend fun getTaskByMessageId(messageId: String): RescueTaskEntity?

    @Query("SELECT * FROM rescue_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): RescueTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: RescueTaskEntity)
}
