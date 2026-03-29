package com.example.resqnet.util

import android.content.Context
import android.provider.Settings

object DeviceIdentity {
    fun resolve(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
    }
}
