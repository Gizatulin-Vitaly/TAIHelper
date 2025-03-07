package com.reftgres.taihelper.ui.model

import com.google.firebase.firestore.DocumentId

data class MeasurementRecord(
    @DocumentId
    val id: String = "", // Уникальный идентификатор записи
    val blockNumber: Int = 0,
    val date: String = "",
    val timestamp: Long = 0,
    val sensors: List<SensorMeasurement> = emptyList()
)

data class SensorMeasurement(
    val sensorTitle: String = "",
    val panelValue: String = "",
    val testoValue: String = "",
    val correctionValue: String = ""
)