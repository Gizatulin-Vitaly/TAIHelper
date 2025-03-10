package com.reftgres.taihelper.data.local.dao

import androidx.room.*
import com.reftgres.taihelper.data.local.entity.MeasurementsEntity

@Dao
interface MeasurementsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: MeasurementsEntity): Long

    @Query("SELECT * FROM measurements_sync_queue WHERE syncStatus = :status")
    suspend fun getPendingMeasurements(status: Int = MeasurementsEntity.SYNC_STATUS_PENDING): List<MeasurementsEntity>

    @Query("UPDATE measurements_sync_queue SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: Int)

    @Query("DELETE FROM measurements_sync_queue WHERE id = :id")
    suspend fun deleteMeasurement(id: Long)
}