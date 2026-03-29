package com.example.resqnet.ui.rescue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.TaskStatus
import kotlinx.coroutines.launch

class RescueTaskManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    val tasks = app.meshRepository.observeAllTasks().asLiveData()

    fun assign(taskId: String, volunteerName: String) {
        viewModelScope.launch {
            app.meshRepository.assignTask(taskId, volunteerName)
        }
    }

    fun updateStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            app.meshRepository.updateTaskStatus(taskId, status)
        }
    }
}
