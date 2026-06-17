package com.example.esttufa.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SensorReadingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: SensorReadingEntity): Long

    @Query("SELECT * FROM sensor_readings ORDER BY created_at DESC")
    suspend fun getAll(): List<SensorReadingEntity>

    @Query(
        """
        SELECT * FROM sensor_readings
        WHERE stove_id = :stoveId
        ORDER BY created_at DESC
        """
    )
    suspend fun getByStoveId(stoveId: String): List<SensorReadingEntity>
}
