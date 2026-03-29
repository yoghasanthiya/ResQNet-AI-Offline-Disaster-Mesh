package com.example.resqnet.ui.volunteer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.resqnet.ResQNetApp
import kotlinx.coroutines.launch

class VolunteerRequestsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    val tasks = app.meshRepository.observeActiveTasks().asLiveData()

    fun accept(taskId: String) {
        viewModelScope.launch {
            app.meshRepository.acceptTask(taskId)
        }
    }
}
