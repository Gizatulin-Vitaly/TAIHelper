package com.reftgres.taihelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "calibrations")
data class CalibrationEntity(
    @PrimaryKey
    val id: String,
    val labSensorValues: List<String>,
    val testSensorValues: List<String>,
    val labAverage: Float,
    val testAverage: Float,
    val resistance: String,
    val constant: Float,
    val r02Resistance: Float,
    val r02I: String,
    val r02SensorValue: String,
    val r05Resistance: Float,
    val r05I: String,
    val r05SensorValue: String,
    val r08Resistance: Float,
    val r08I: String,
    val r08SensorValue: String,
    val r40DegResistance: Float,
    val r40DegSensorValue: String,
    val timestamp: Date,
    val userId: String,
    // Поле для отслеживания синхронизации
    val isSynced: Boolean = true
)