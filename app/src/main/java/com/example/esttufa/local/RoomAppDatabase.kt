package com.example.esttufa.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SensorReadingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RoomAppDatabase : RoomDatabase() {

    abstract fun sensorReadingDao(): SensorReadingDao

    companion object {
        private const val DATABASE_NAME = "esttufa-local.db"

        @Volatile
        private var instance: RoomAppDatabase? = null

        fun getInstance(context: Context): RoomAppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoomAppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
