package com.example.resqnet.data.db

import androidx.room.TypeConverter
import com.example.resqnet.data.model.DeliveryStatus
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.data.model.TaskStatus
import com.example.resqnet.data.model.UserRole
import com.example.resqnet.data.model.VolunteerSkill

class RoomConverters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)

    @TypeConverter
    fun fromDeliveryStatus(value: DeliveryStatus): String = value.name

    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus = DeliveryStatus.valueOf(value)

    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromVolunteerSkill(value: VolunteerSkill?): String? = value?.name

    @TypeConverter
    fun toVolunteerSkill(value: String?): VolunteerSkill? = value?.takeIf { it.isNotBlank() }?.let {
        VolunteerSkill.valueOf(it)
    }

    @TypeConverter
    fun fromVolunteerSkills(value: List<VolunteerSkill>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toVolunteerSkills(value: String): List<VolunteerSkill> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { VolunteerSkill.valueOf(it) }
    }
}
