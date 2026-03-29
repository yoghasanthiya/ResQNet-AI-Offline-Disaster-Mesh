package com.example.resqnet.ui.registration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.UserRole
import com.example.resqnet.data.model.VolunteerSkill
import kotlinx.coroutines.launch

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp

    fun saveProfile(
        name: String,
        phone: String,
        role: UserRole,
        locationLabel: String,
        skills: List<VolunteerSkill>
    ) {
        viewModelScope.launch {
            app.userRepository.register(
                name = name,
                phoneNumber = phone,
                role = role,
                locationLabel = locationLabel,
                skills = skills
            )
        }
    }
}
