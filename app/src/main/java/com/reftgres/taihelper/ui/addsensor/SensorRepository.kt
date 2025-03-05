package com.reftgres.taihelper.ui.addsensor

interface SensorRepository {

    data class Block(val id: String, val name: String)
    data class SensorType(val id: String, val name: String)
    data class Measurement(val id: String, val name: String)
    data class OutputRange(val id: String, val name: String)
    suspend fun getBlocks(): List<Block>
    suspend fun getSensorTypes(): List<SensorType>
    suspend fun getMeasurementStarts(): List<Measurement>
    suspend fun getMeasurementEnds(): List<Measurement>
    fun getOutputRanges(): List<OutputRange>
    suspend fun saveSensor(
        typeId: String,
        position: String,
        outputScale: String,
        midPoint: String,
        modification: String,
        blockId: String,
        measurementStartId: String,
        measurementEndId: String
    ): Result<String>
}