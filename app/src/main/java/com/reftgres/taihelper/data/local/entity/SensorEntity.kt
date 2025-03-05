package com.reftgres.taihelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sensors")
data class SensorEntity(
    @PrimaryKey
    val id: String,
    val position: String,
    val serialNumber: String,
    val midPoint: String,
    val outputScale: String,
    val blockId: String,
    val type: String,
    val measurementStartId: String,
    val measurementEndId: String,
    val modification: String,
    val lastCalibration: Date? = null,
    val isSynced: Boolean = true
)