package com.example.resqnet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.resqnet.data.model.UserRole
import com.example.resqnet.data.model.VolunteerSkill

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phoneNumber: String,
    val role: UserRole,
    val locationLabel: String,
    val skills: List<VolunteerSkill>
)
