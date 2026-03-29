package com.example.resqnet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfileEntity::class,
        EmergencyMessageEntity::class,
        RescueTaskEntity::class,
        KnownDeviceEntity::class,
        PendingAckEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class ResQNetDatabase : RoomDatabase() {
    abstract fun resQNetDao(): ResQNetDao

    companion object {
        fun create(context: Context): ResQNetDatabase {
            return Room.databaseBuilder(
                context,
                ResQNetDatabase::class.java,
                "resqnet.db"
            ).fallbackToDestructiveMigration().build()
        }
    }
}
