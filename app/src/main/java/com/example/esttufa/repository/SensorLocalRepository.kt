package com.example.esttufa.repository

import android.content.Context
import com.example.esttufa.local.RoomAppDatabase
import com.example.esttufa.local.SensorReadingDao
import com.example.esttufa.local.SensorReadingEntity
import com.example.esttufa.model.IrrigationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorLocalRepository(
    private val sensorReadingDao: SensorReadingDao
) {

    suspend fun saveSensorReading(
        stoveId: String?,
        response: IrrigationResponse
    ): Long = withContext(Dispatchers.IO) {
        sensorReadingDao.insert(response.toEntity(stoveId))
    }

    private fun IrrigationResponse.toEntity(stoveId: String?): SensorReadingEntity =
        SensorReadingEntity(
            stoveId = stoveId,
            crop = crop,
            version = version,
            moisture = moisture,
            temperature = temperature,
            light = light,
            irrigationTime = irrigation_time,
            className = class_name
        )

    companion object {
        fun create(context: Context): SensorLocalRepository {
            val database = RoomAppDatabase.getInstance(context)
            return SensorLocalRepository(database.sensorReadingDao())
        }
    }
}
