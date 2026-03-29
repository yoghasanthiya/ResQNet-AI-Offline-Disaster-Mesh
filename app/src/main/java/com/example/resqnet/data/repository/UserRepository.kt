package com.example.resqnet.data.repository

import com.example.resqnet.data.db.ResQNetDao
import com.example.resqnet.data.db.UserProfileEntity
import com.example.resqnet.data.model.UserProfile
import com.example.resqnet.data.model.UserRole
import com.example.resqnet.data.model.VolunteerSkill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(
    private val dao: ResQNetDao
) {
    fun observeUserProfile(): Flow<UserProfile?> {
        return dao.observeUserProfile().map { entity ->
            entity?.let {
                UserProfile(
                    name = it.name,
                    phoneNumber = it.phoneNumber,
                    role = it.role,
                    locationLabel = it.locationLabel,
                    skills = it.skills
                )
            }
        }
    }

    suspend fun getUserProfile(): UserProfile? {
        return dao.getUserProfile()?.let {
            UserProfile(
                name = it.name,
                phoneNumber = it.phoneNumber,
                role = it.role,
                locationLabel = it.locationLabel,
                skills = it.skills
            )
        }
    }

    suspend fun register(
        name: String,
        phoneNumber: String,
        role: UserRole,
        locationLabel: String,
        skills: List<VolunteerSkill>
    ) {
        dao.upsertUserProfile(
            UserProfileEntity(
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                role = role,
                locationLabel = locationLabel.trim(),
                skills = skills
            )
        )
    }
}
