package com.reftgres.taihelper.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.reftgres.taihelper.data.local.entity.CalibrationEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibrations ORDER BY timestamp DESC")
    fun getAllCalibrations(): Flow<List<CalibrationEntity>>

    @Query("SELECT * FROM calibrations WHERE id = :id")
    suspend fun getCalibrationById(id: String): CalibrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibration(calibration: CalibrationEntity)

    @Update
    suspend fun updateCalibration(calibration: CalibrationEntity)

    @Delete
    suspend fun deleteCalibration(calibration: CalibrationEntity)

    @Query("UPDATE calibrations SET isSynced = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: String, synced: Boolean)

    @Query("SELECT * FROM calibrations WHERE isSynced = 0")
    suspend fun getUnsyncedCalibrations(): List<CalibrationEntity>

    @Query("SELECT * FROM calibrations ORDER BY timestamp DESC")
    fun getAllCalibrationsLive(): LiveData<List<CalibrationEntity>>

}