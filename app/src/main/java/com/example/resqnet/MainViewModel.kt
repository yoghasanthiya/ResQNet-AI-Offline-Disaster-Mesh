package com.example.resqnet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.map

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    val hasRegisteredUser = app.userRepository.observeUserProfile()
        .map { it != null }
        .asLiveData()
}
