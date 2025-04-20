package com.reftgres.taihelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reftgres.taihelper.ui.ajk.DataAjk
import java.util.Date

@Entity(tableName = "calibrations")
data class CalibrationEntity(
    @PrimaryKey
    val id: String,
    val sensorPosition: String,
    val sensorSerial: String,
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

fun CalibrationEntity.toDataAjk(): DataAjk {
    return DataAjk(
        id = id,
        sensorPosition = sensorPosition,
        sensorSerial = sensorSerial,
        labSensorValues = labSensorValues,
        testSensorValues = testSensorValues,
        labAverage = labAverage,
        testAverage = testAverage,
        resistance = resistance,
        constant = constant,
        r02Resistance = r02Resistance,
        r02I = r02I,
        r02SensorValue = r02SensorValue,
        r05Resistance = r05Resistance,
        r05I = r05I,
        r05SensorValue = r05SensorValue,
        r08Resistance = r08Resistance,
        r08I = r08I,
        r08SensorValue = r08SensorValue,
        r40DegResistance = r40DegResistance,
        r40DegSensorValue = r40DegSensorValue,
        timestamp = timestamp,
        userId = userId
    )
}