package com.example.resqnet.data.model

data class UserProfile(
    val name: String,
    val phoneNumber: String,
    val role: UserRole,
    val locationLabel: String,
    val skills: List<VolunteerSkill>
)
