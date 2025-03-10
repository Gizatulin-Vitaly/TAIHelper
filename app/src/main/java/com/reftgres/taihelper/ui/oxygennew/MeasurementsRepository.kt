package com.reftgres.taihelper.ui.oxygennew


import com.reftgres.taihelper.ui.model.MeasurementRecord
import kotlin.Result

interface MeasurementsRepository {
    suspend fun saveMeasurement(measurement: MeasurementRecord): Result<String>
    suspend fun getMeasurements(): Result<List<MeasurementRecord>>
    suspend fun getMeasurementById(id: String): Result<MeasurementRecord?>
    suspend fun updateSensorMidpoint(blockNumber: Int, sensorTitle: String, midpointValue: String): Result<Boolean>
    suspend fun saveMeasurementOffline(measurement: MeasurementRecord, sensorUpdates: List<SensorUpdate>): Result<String>

    data class SensorUpdate(
        val blockReference: String,
        val position: String,
        val midpointValue: String
    )
}