package com.example.resqnet.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.resqnet.ResQNetApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeshForegroundService : android.app.Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        MeshNotificationHelper.ensureChannels(this)
        runtimeState.value = runtimeState.value.copy(isRunning = true)
        startForeground(
            MeshNotificationHelper.SERVICE_NOTIFICATION_ID,
            serviceNotification()
        )
        startMeshRuntime()
        startNotificationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_APP_BACKGROUND -> runtimeState.value = runtimeState.value.copy(isBackgroundMode = true)
            ACTION_APP_FOREGROUND -> runtimeState.value = runtimeState.value.copy(isBackgroundMode = false)
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        updateForegroundNotification()
        startMeshRuntime()
        return START_STICKY
    }

    override fun onDestroy() {
        updateJob?.cancel()
        scope.cancel()
        val app = application as ResQNetApp
        app.meshCoordinator.stop()
        app.liveLocationTracker.stop()
        runtimeState.value = MeshRuntimeState()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun startMeshRuntime() {
        val app = application as ResQNetApp
        app.liveLocationTracker.start()
        app.meshCoordinator.start()
        updateForegroundNotification()
    }

    private fun startNotificationUpdates() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (true) {
                updateForegroundNotification()
                delay(10_000)
            }
        }
    }

    private fun updateForegroundNotification() {
        val manager = ContextCompat.getSystemService(this, android.app.NotificationManager::class.java) ?: return
        manager.notify(MeshNotificationHelper.SERVICE_NOTIFICATION_ID, serviceNotification())
    }

    private fun serviceNotification() = MeshNotificationHelper.buildServiceNotification(
        context = this,
        status = (application as ResQNetApp).meshCoordinator.status.value,
        connectedPeers = (application as ResQNetApp).meshCoordinator.peers.value.count { it.isConnected },
        isBackgroundMode = runtimeState.value.isBackgroundMode
    )

    companion object {
        private const val ACTION_APP_FOREGROUND = "com.example.resqnet.action.APP_FOREGROUND"
        private const val ACTION_APP_BACKGROUND = "com.example.resqnet.action.APP_BACKGROUND"
        private const val ACTION_STOP_SERVICE = "com.example.resqnet.action.STOP_SERVICE"

        private val runtimeState = MutableStateFlow(MeshRuntimeState())
        val serviceState: StateFlow<MeshRuntimeState> = runtimeState

        fun start(context: Context) {
            val intent = Intent(context, MeshForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifyForeground(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MeshForegroundService::class.java).setAction(ACTION_APP_FOREGROUND)
            )
        }

        fun notifyBackground(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MeshForegroundService::class.java).setAction(ACTION_APP_BACKGROUND)
            )
        }

        fun stop(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MeshForegroundService::class.java).setAction(ACTION_STOP_SERVICE)
            )
        }
    }
}

data class MeshRuntimeState(
    val isRunning: Boolean = false,
    val isBackgroundMode: Boolean = false
)
