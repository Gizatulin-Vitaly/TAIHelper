package com.reftgres.taihelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements_sync_queue")
data class MeasurementsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val measurementId: String,
    val blockNumber: Int,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: Int = SYNC_STATUS_PENDING,
    val sensorData: String
) {
    companion object {
        const val SYNC_STATUS_PENDING = 0
        const val SYNC_STATUS_SYNCING = 1
        const val SYNC_STATUS_SYNCED = 2
        const val SYNC_STATUS_ERROR = 3
    }
}