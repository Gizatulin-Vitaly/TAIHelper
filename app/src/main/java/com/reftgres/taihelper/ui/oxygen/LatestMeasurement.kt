package com.reftgres.taihelper.ui.oxygen

import com.reftgres.taihelper.ui.model.SensorMeasurement
import java.util.Date

data class LatestMeasurement(
    val id: String = "",
    val date: String = "",
    val blockNumber: Int = 0,
    val sensors: List<SensorMeasurement> = emptyList(),
    val timestamp: Long = 0
)

data class SensorMeasurements(
    val id: String = "",
    val date: Date = Date(),
    val blockNumber: Int = 0,
    val sensorValues: List<SensorValue> = emptyList()
)

data class SensorValue(
    val sensorId: String = "",
    val position: String = "",
    val panelValue: String = "",
    val testoValue: String = "",
    val correctionValue: String = "",
    val timestamp: Long = System.currentTimeMillis()
)