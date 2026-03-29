package com.example.resqnet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.resqnet.MainActivity
import com.example.resqnet.R
import com.example.resqnet.data.model.MessageType

object MeshNotificationHelper {
    const val SERVICE_CHANNEL_ID = "resqnet_mesh_service"
    const val MESSAGE_CHANNEL_ID = "resqnet_incoming_messages"
    const val SERVICE_NOTIFICATION_ID = 2001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Mesh background service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Bluetooth mesh communication active in the background."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "Incoming emergency messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts you when new emergency messages arrive over the mesh."
            }
        )
    }

    fun buildServiceNotification(
        context: Context,
        status: String,
        connectedPeers: Int,
        isBackgroundMode: Boolean
    ): Notification {
        val launchIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val modeText = if (isBackgroundMode) "Background mode active" else "Foreground monitoring"
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ResQNet mesh active")
            .setContentText("$status • $connectedPeers connected peers • $modeText")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$status\nConnected peers: $connectedPeers\nMode: $modeText"
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(launchIntent)
            .build()
    }

    fun showIncomingMessage(
        context: Context,
        senderName: String,
        content: String,
        type: MessageType
    ) {
        ensureChannels(context)
        val launchIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val preview = if (content.length > 90) "${content.take(87)}..." else content
        val title = if (type == MessageType.SOS) {
            "SOS from $senderName"
        } else {
            "$senderName • ${type.name}"
        }
        NotificationManagerCompat.from(context).notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(preview)
                .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
                .setPriority(
                    if (type == MessageType.SOS) {
                        NotificationCompat.PRIORITY_MAX
                    } else {
                        NotificationCompat.PRIORITY_HIGH
                    }
                )
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(launchIntent)
                .build()
        )
    }
}
