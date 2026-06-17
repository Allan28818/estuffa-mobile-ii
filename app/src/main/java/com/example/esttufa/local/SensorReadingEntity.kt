package com.example.esttufa.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_readings")
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "stove_id")
    val stoveId: String?,
    val crop: String,
    val version: String,
    val moisture: Double,
    val temperature: Double,
    val light: Double,
    @ColumnInfo(name = "irrigation_time")
    val irrigationTime: Double,
    @ColumnInfo(name = "class_name")
    val className: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
