package com.reftgres.taihelper.data.local.dao

import androidx.room.*
import com.reftgres.taihelper.data.local.entity.SensorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Query("SELECT * FROM sensors")
    fun getAllSensors(): Flow<List<SensorEntity>>

    @Query("SELECT * FROM sensors WHERE blockId = :blockId")
    suspend fun getSensorsByBlock(blockId: String): List<SensorEntity>

    @Query("SELECT * FROM sensors WHERE id = :id")
    suspend fun getSensorById(id: String): SensorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensor(sensor: SensorEntity)

    @Update
    suspend fun updateSensor(sensor: SensorEntity)

    @Delete
    suspend fun deleteSensor(sensor: SensorEntity)

    @Query("UPDATE sensors SET isSynced = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: String, synced: Boolean)

    @Query("SELECT * FROM sensors WHERE isSynced = 0")
    suspend fun getUnsyncedSensors(): List<SensorEntity>
}