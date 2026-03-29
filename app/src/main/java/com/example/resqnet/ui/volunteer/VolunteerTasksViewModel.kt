package com.example.resqnet.ui.volunteer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.resqnet.ResQNetApp
import com.example.resqnet.data.model.TaskStatus
import com.example.resqnet.util.DeviceIdentity
import kotlinx.coroutines.launch

class VolunteerTasksViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ResQNetApp
    val tasks = liveData {
        val profile = app.userRepository.getUserProfile()
        if (profile == null) {
            emit(emptyList())
        } else {
            emitSource(
                app.meshRepository.observeVolunteerTasks(
                    volunteerId = DeviceIdentity.resolve(application),
                    volunteerName = profile.name
                ).asLiveData()
            )
        }
    }

    fun updateStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            app.meshRepository.updateTaskStatus(taskId, status)
        }
    }
}
